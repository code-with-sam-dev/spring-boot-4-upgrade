#!/usr/bin/env bash
#
# Replays the upgrade one step at a time and records what really happens.
#
#   scripts/verify.sh            run every step
#   scripts/verify.sh step-3     run one step (step-0 ... step-6, or jars)
#
# Each step is checked out into its own git worktree, so your working copy
# is never touched. Transcripts land in evidence/<step>/*.txt. Full Maven and
# application logs land in evidence/.raw/ (not committed).
#
# Needs: Docker running, ports 8080 and 5442 free, and a JDK 21.0.8+ or 22+
# (NullAway's JSpecify mode needs it). Set JAVA_HOME to choose one.

set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
EVIDENCE="$ROOT/evidence"
RAW="$EVIDENCE/.raw"
mkdir -p "${TMPDIR:-/tmp}/spring-boot-4-upgrade-verify"
WORK="$(cd "${TMPDIR:-/tmp}/spring-boot-4-upgrade-verify" && pwd -P)"
COMPOSE_PROJECT=sb4-upgrade-verify
APP_PID=""

# VERIFY_JAVA_HOME wins. Otherwise use JAVA_HOME, unless it is a Java 21 older
# than 21.0.8 and sdkman has 21.0.10-zulu, which the recordings used.
if [[ -n "${VERIFY_JAVA_HOME:-}" ]]; then
    export JAVA_HOME="$VERIFY_JAVA_HOME"
elif [[ -d "$HOME/.sdkman/candidates/java/21.0.10-zulu" ]]; then
    current=$("${JAVA_HOME:-/usr}/bin/java" -version 2>&1 | sed -n 's/.*version "\([^"]*\)".*/\1/p' | head -1)
    case "$current" in
        21.0.[0-7]|21.0.[0-7][!0-9]*|"") export JAVA_HOME="$HOME/.sdkman/candidates/java/21.0.10-zulu" ;;
    esac
fi
[[ -n "${JAVA_HOME:-}" ]] && export PATH="$JAVA_HOME/bin:$PATH"

mkdir -p "$RAW" "$WORK"

# ---------------------------------------------------------------- helpers

log() { printf '\n== %s\n' "$*" >&2; }

cleanup() {
    stop_app
    docker compose -p "$COMPOSE_PROJECT" -f "$ROOT/compose.yaml" down -v >/dev/null 2>&1
    git -C "$ROOT" worktree prune
}
trap cleanup EXIT

# checkout <ref> <name>: a fresh worktree for <ref>, path printed on stdout
checkout() {
    local dir="$WORK/$2"
    git -C "$ROOT" worktree remove --force "$dir" >/dev/null 2>&1
    rm -rf "$dir"
    git -C "$ROOT" worktree add --detach "$dir" "$1" >/dev/null 2>&1 || { echo "cannot check out $1" >&2; exit 1; }
    echo "$1" >"$dir.ref"
    echo "$dir"
}

release() {
    git -C "$ROOT" worktree remove --force "$WORK/$1" >/dev/null 2>&1
    rm -f "$WORK/$1.ref"
}

header() {
    local dir="$1"
    echo "# ref: $(cat "$dir.ref" 2>/dev/null) ($(git -C "$dir" rev-parse --short HEAD))"
    echo "# commit: $(git -C "$dir" log -1 --format=%s)"
    echo "# spring boot parent: $(sed -n 's:.*<version>\(.*\)</version>.*:\1:p' "$dir/pom.xml" | head -1)"
    echo "# java: $(java -version 2>&1 | head -1)"
    echo "# recorded: $(date '+%Y-%m-%d %H:%M %Z')"
    echo
}

# mvn <dir> <rawname> <args...>: runs the wrapper, full output to RAW, prints the exit code line
mvn_run() {
    local dir="$1" raw="$2"; shift 2
    (cd "$dir" && ./mvnw -B "$@") >"$RAW/$raw.log" 2>&1
    local code=$?
    echo "\$ ./mvnw $*"
    return $code
}

# The lines of a Maven log that matter, see trim-build.py.
trim_build() {
    python3 "$ROOT/scripts/trim-build.py" "$RAW/$1.log" "$2" "$ROOT" "$WORK"
}

db_reset() {
    docker compose -p "$COMPOSE_PROJECT" -f "$ROOT/compose.yaml" down -v >/dev/null 2>&1
    docker compose -p "$COMPOSE_PROJECT" -f "$ROOT/compose.yaml" up -d --wait >/dev/null 2>&1 \
        || { echo "postgres did not start" >&2; exit 1; }
}

# start_app <dir> <rawname>: runs the packaged jar against a fresh database
start_app() {
    local dir="$1" raw="$2"
    db_reset
    local jar
    jar=$(ls "$dir"/target/orders-*.jar | grep -v plain | head -1)
    java -jar "$jar" --spring.profiles.active=local-stub >"$RAW/$raw.log" 2>&1 &
    APP_PID=$!
    for _ in $(seq 1 90); do
        grep -q "Started OrdersApplication" "$RAW/$raw.log" && return 0
        kill -0 "$APP_PID" 2>/dev/null || return 1
        sleep 1
    done
    return 1
}

stop_app() {
    if [[ -n "$APP_PID" ]]; then
        kill "$APP_PID" 2>/dev/null
        wait "$APP_PID" 2>/dev/null
        APP_PID=""
    fi
}

# The startup lines worth showing, and the properties migrator report if any.
trim_startup() {
    local raw="$RAW/$1.log"
    grep -E ":: Spring Boot ::|Starting OrdersApplication|(Tomcat|Undertow) started|Tomcat initialized|Undertow|Flyway|flyway|Migrating schema|Successfully (applied|validated)|Started OrdersApplication|APPLICATION FAILED" "$raw" \
        | sed -E -e 's/^[0-9T:.+-]+ +//' -e 's# \(/[^)]*/target/(orders-[^ ]*\.jar) started by [^)]*\)# (target/\1)#'
    if grep -q "PropertiesMigrationListener" "$raw"; then
        echo
        echo "--- properties migrator report ---"
        awk '/PropertiesMigrationListener/{p=1} p{print} /Please refer to the release notes/{exit}' "$raw" | sed -E 's/^[0-9T:.+-]+ +//'
    else
        echo
        echo "--- properties migrator: no report printed ---"
    fi
}

# req <description> <curl args...>: prints the request and the full response
req() {
    local what="$1"; shift
    echo "### $what"
    local shown=""
    for a in "$@"; do
        if [[ "$a" == *" "* || "$a" == *"{"* ]]; then shown+=" '$a'"; else shown+=" $a"; fi
    done
    echo "\$ curl -i$shown"
    curl -s -i --max-time 10 "$@" | tr -d '\r'
    printf '\n\n'
}

SARAH='{"customer_name":"Sarah Thompson","currency":"GBP","items":[{"sku":"KETTLE-01","quantity":1,"unit_price":24.50},{"sku":"MUG-02","quantity":2,"unit_price":6.00}]}'
SARAH_CAMEL='{"customerName":"Sarah Thompson","currency":"GBP","items":[{"sku":"KETTLE-01","quantity":1,"unitPrice":24.50},{"sku":"MUG-02","quantity":2,"unitPrice":6.00}]}'
BASE=http://localhost:8080

post_sarah() { req "${2:-POST /orders}" -X POST "$BASE/orders" -H 'Content-Type: application/json' -d "${1:-$SARAH}"; }

jar_listing() {
    local jar
    jar=$(ls "$1"/target/orders-*.jar | grep -v plain | head -1)
    unzip -l "$jar" 'BOOT-INF/lib/*' | awk 'NR>3 && $4 ~ /BOOT-INF/ {sub("BOOT-INF/lib/","",$4); printf "%10d  %s\n", $1, $4}' | sort -k2
}

# ---------------------------------------------------------------- steps

step0() {
    local out="$EVIDENCE/step-0" d
    mkdir -p "$out"; log "step-0: Spring Boot 3.5.16 baseline"
    d=$(checkout step-0-boot-3.5 step-0)
    { header "$d"; echo "Compiler deprecation warnings on the untouched 3.5 app:"; echo
      mvn_run "$d" step-0-deprecation clean test-compile -Dmaven.compiler.showDeprecation=true -Dmaven.compiler.showWarnings=true
      grep -E "^\[WARNING\] /.*\.java" "$RAW/step-0-deprecation.log" | sed "s#$d/##g"; } >"$out/deprecation.txt"
    { header "$d"; mvn_run "$d" step-0-build clean verify; trim_build step-0-build "$d"; } >"$out/build.txt"
    if start_app "$d" step-0-app; then
        { header "$d"; trim_startup step-0-app; } >"$out/startup.txt"
        { header "$d"; post_sarah; req "GET /orders/1" "$BASE/orders/1"; req "GET /orders/999 (unknown)" "$BASE/orders/999"
          req "GET /actuator/health" "$BASE/actuator/health"; req "GET /actuator/health/liveness" "$BASE/actuator/health/liveness"; } >"$out/curl.txt"
    else
        { header "$d"; echo "APP DID NOT START"; tail -40 "$RAW/step-0-app.log"; } >"$out/startup.txt"
    fi
    stop_app; release step-0
}

step1() {
    local out="$EVIDENCE/step-1" d
    mkdir -p "$out"; log "step-1: latest 3.5, deprecations removed, migrator added"
    d=$(checkout step-1-latest-3.5 step-1)
    { header "$d"
      echo "Newest 3.5.x releases on Maven Central:"
      curl -s https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/maven-metadata.xml \
          | grep -oE "<version>3\.5\.[0-9]+</version>" | sed -E 's/<\/?version>//g' | sort -t. -k3 -n | tail -3
      echo; mvn_run "$d" step-1-build clean verify; trim_build step-1-build "$d"
      echo; echo "Deprecation warnings left (showDeprecation is on in the pom): $(grep -cE '^\[WARNING\] /.*\.java' "$RAW/step-1-build.log")"; } >"$out/build.txt"
    if start_app "$d" step-1-app; then
        { header "$d"; trim_startup step-1-app; } >"$out/startup.txt"
        { header "$d"; post_sarah; req "GET /orders/1" "$BASE/orders/1"; req "GET /orders/999 (unknown)" "$BASE/orders/999"; } >"$out/curl.txt"
    fi
    stop_app; release step-1
}

step2() {
    local out="$EVIDENCE/step-2" d
    mkdir -p "$out"; log "step-2: parent to 4.1.1 only"
    d=$(checkout step-2-boot-4.1-parent step-2)
    { header "$d"; git -C "$d" diff HEAD~1 -- pom.xml; echo; mvn_run "$d" step-2-build clean verify; trim_build step-2-build "$d"
      echo; echo "Maven Central, does the Undertow starter exist?"
      for v in 3.5.16 4.0.0 4.1.1; do
          printf '  spring-boot-starter-undertow %-7s HTTP %s\n' "$v" \
              "$(curl -s -o /dev/null -w '%{http_code}' https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-starter-undertow/$v/spring-boot-starter-undertow-$v.pom)"
      done; } >"$out/build.txt"
    release step-2

    # What a 3.5 test suite that still uses @MockBean meets on 4.1: the 3.5
    # tests from step 0, compiled against the step 3b code (main compiles).
    d=$(checkout step-3b-jackson-imports step-2-mockbean)
    git -C "$d" checkout step-0-boot-3.5 -- src/test >/dev/null 2>&1
    { header "$d"
      echo "Experiment: step 1 skipped. The untouched 3.5 test sources (with @MockBean) compiled against Spring Boot 4.1.1"
      echo "(main code from step-3b-jackson-imports so that test compilation is reached)."; echo
      mvn_run "$d" step-2-mockbean clean test-compile; trim_build step-2-mockbean "$d"; } >"$out/mockbean-if-step-1-skipped.txt"
    release step-2-mockbean
}

step3() {
    local out="$EVIDENCE/step-3" d
    mkdir -p "$out"; log "step-3: loud breaks fixed one layer at a time"
    d=$(checkout step-3a-tomcat step-3a)
    { header "$d"; mvn_run "$d" step-3a-build clean verify; trim_build step-3a-build "$d"; } >"$out/3a-tomcat-build.txt"; release step-3a
    d=$(checkout step-3b-jackson-imports step-3b)
    { header "$d"; mvn_run "$d" step-3b-build clean verify; trim_build step-3b-build "$d"; } >"$out/3b-jackson-build.txt"; release step-3b
    d=$(checkout step-3c-test-starters step-3c)
    { header "$d"; mvn_run "$d" step-3c-build clean verify; trim_build step-3c-build "$d"
      echo; echo "The JSON the failing test saw:"; grep -E "^ +Body = \{" "$RAW/step-3c-build.log" | head -2; } >"$out/3c-test-starters-build.txt"; release step-3c

    d=$(checkout step-3-loud-fixed step-3)
    { header "$d"
      echo "What most people run before shipping a jar: unit and slice tests (surefire)."; echo
      mvn_run "$d" step-3-package clean package; trim_build step-3-package "$d"; } >"$out/build-package.txt"
    if start_app "$d" step-3-app; then
        { header "$d"; trim_startup step-3-app; } >"$out/startup.txt"
        { header "$d"; post_sarah "" "POST /orders  (the first order after the upgrade)"
          req "GET /actuator/health" "$BASE/actuator/health"; req "GET /actuator/health/liveness" "$BASE/actuator/health/liveness"
          echo "### application log around the failure"
          grep -E "ERROR|relation \"orders\"|bad SQL|SQL Error" "$RAW/step-3-app.log" | grep -v PropertiesMigrationListener | sed -E 's/^[0-9T:.+-]+ +//' | head -6; } >"$out/curl.txt"
    else
        { header "$d"; echo "APP DID NOT START"; tail -40 "$RAW/step-3-app.log"; } >"$out/startup.txt"
    fi
    stop_app
    { header "$d"
      echo "The same commit with the Testcontainers integration tests (failsafe) included."; echo
      mvn_run "$d" step-3-verify verify; trim_build step-3-verify "$d"; } >"$out/build-verify.txt"
    release step-3
}

step4() {
    local out="$EVIDENCE/step-4" d
    mkdir -p "$out"; log "step-4: spring-boot-starter-flyway"
    d=$(checkout step-4-flyway-starter step-4)
    { header "$d"; git -C "$d" diff HEAD~1 -- pom.xml; echo; mvn_run "$d" step-4-build clean verify; trim_build step-4-build "$d"; } >"$out/build.txt"
    if start_app "$d" step-4-app; then
        { header "$d"; trim_startup step-4-app; } >"$out/startup.txt"
        { header "$d"; post_sarah; req "GET /orders/1" "$BASE/orders/1"; req "GET /orders/999 (unknown)" "$BASE/orders/999"; } >"$out/curl.txt"
    fi
    stop_app; release step-4
}

# Same requests against 3.5 (step 1), the ObjectMapper probe (5a) and the
# customizer (5b), so the JSON can be compared byte for byte.
json_probe() {
    local d="$1" name="$2" out="$3"
    if start_app "$d" "$name-app"; then
        {
            header "$d"
            post_sarah "$SARAH" "POST /orders, snake_case body (the documented contract)"
            post_sarah "$SARAH_CAMEL" "POST /orders, camelCase body"
            req "GET /orders/1" "$BASE/orders/1"
        } >"$out/json-$name.txt"
        curl -s "$BASE/orders/1" >"$out/.body-$name.json"
    fi
    stop_app
}

step5() {
    local out="$EVIDENCE/step-5" d
    mkdir -p "$out"; log "step-5: Jackson probe and customizer"
    d=$(checkout step-1-latest-3.5 step-5-35)
    mvn_run "$d" step-5-35-package clean package -DskipTests >/dev/null; json_probe "$d" boot-3.5 "$out"; release step-5-35

    d=$(checkout step-5a-objectmapper-probe step-5a)
    { header "$d"; mvn_run "$d" step-5a-build clean package; trim_build step-5a-build "$d"; } >"$out/5a-objectmapper-build.txt"
    mvn_run "$d" step-5a-package clean package -DskipTests >/dev/null; json_probe "$d" boot-4.1-objectmapper-bean "$out"; release step-5a

    d=$(checkout step-5b-customizer step-5b)
    { header "$d"; mvn_run "$d" step-5b-build clean verify; trim_build step-5b-build "$d"; } >"$out/5b-customizer-build.txt"
    json_probe "$d" boot-4.1-customizer "$out"; release step-5b

    {
        echo "# GET /orders/1 body, byte for byte"
        echo
        for n in boot-3.5 boot-4.1-objectmapper-bean boot-4.1-customizer; do
            printf '%-28s %s\n' "$n:" "$(cat "$out/.body-$n.json" 2>/dev/null)"
        done
        echo
        if cmp -s "$out/.body-boot-3.5.json" "$out/.body-boot-4.1-customizer.json"; then
            echo "3.5 and 4.1 with the customizer: IDENTICAL"
        else
            echo "3.5 and 4.1 with the customizer: DIFFERENT"
            diff <(tr ',' '\n' <"$out/.body-boot-3.5.json") <(tr ',' '\n' <"$out/.body-boot-4.1-customizer.json")
        fi
        if cmp -s "$out/.body-boot-3.5.json" "$out/.body-boot-4.1-objectmapper-bean.json"; then
            echo "3.5 and 4.1 with the ObjectMapper bean: IDENTICAL"
        else
            echo "3.5 and 4.1 with the ObjectMapper bean: DIFFERENT"
        fi
        echo
        echo "Note: in the ObjectMapper-bean run the snake_case POST is rejected, so order 1 is the camelCase one."
    } >"$out/json-compare.txt"
    rm -f "$out"/.body-*.json
}

step6() {
    local out="$EVIDENCE/step-6" d
    mkdir -p "$out"; log "step-6: opt-in features"
    d=$(checkout step-6c-nullaway-fails step-6c)
    { header "$d"; mvn_run "$d" step-6c-build clean compile; trim_build step-6c-build "$d"; } >"$out/6c-nullaway-build.txt"; release step-6c
    d=$(checkout step-6d-nullaway-fixed step-6d)
    { header "$d"; mvn_run "$d" step-6d-build clean verify; trim_build step-6d-build "$d"; } >"$out/6d-nullaway-fixed-build.txt"; release step-6d

    d=$(checkout step-6-opt-in step-6)
    { header "$d"; mvn_run "$d" step-6-build clean verify; trim_build step-6-build "$d"; } >"$out/build.txt"
    if start_app "$d" step-6-app; then
        { header "$d"; trim_startup step-6-app; } >"$out/startup.txt"
        {
            header "$d"
            post_sarah "" "POST /orders (payments called through the @HttpExchange client)"
            req "GET /orders/1, no X-Version header (default 1)" "$BASE/orders/1"
            req "GET /orders/1, X-Version: 1 (deprecated)" "$BASE/orders/1" -H 'X-Version: 1'
            req "GET /orders/1, X-Version: 2" "$BASE/orders/1" -H 'X-Version: 2'
            req "GET /orders/1, X-Version: 3 (unsupported)" "$BASE/orders/1" -H 'X-Version: 3'
            req "GET /orders/1, X-Version: abc (not a version)" "$BASE/orders/1" -H 'X-Version: abc'
            req "GET /orders/999 (unknown, error message back after the property rename)" "$BASE/orders/999"
        } >"$out/curl.txt"
    fi
    stop_app; release step-6
}

jars() {
    local out="$EVIDENCE/jars" a b
    mkdir -p "$out"; log "jars: BOOT-INF/lib on 3.5 and 4.1"
    a=$(checkout step-1-latest-3.5 jars-35); mvn_run "$a" jars-35 clean package -DskipTests >/dev/null
    b=$(checkout step-6-opt-in jars-41); mvn_run "$b" jars-41 clean package -DskipTests >/dev/null
    { header "$a"; jar_listing "$a"; } >"$out/boot-3.5-lib.txt"
    { header "$b"; jar_listing "$b"; } >"$out/boot-4.1-lib.txt"
    {
        echo "# BOOT-INF/lib, Spring Boot jars only"
        echo
        echo "## 3.5.16 (step-1-latest-3.5)"; jar_listing "$a" | grep " spring-boot"
        echo; echo "## 4.1.1 (step-6-opt-in, same features plus the opt-ins)"; jar_listing "$b" | grep " spring-boot"
        echo
        for n in "3.5:$a" "4.1:$b"; do
            local dir="${n#*:}"
            printf '%s  all jars: %3d, %s bytes | spring-boot jars: %2d, %s bytes\n' "${n%%:*}" \
                "$(jar_listing "$dir" | wc -l | tr -d ' ')" "$(jar_listing "$dir" | awk '{s+=$1} END {print s}')" \
                "$(jar_listing "$dir" | grep -c ' spring-boot')" "$(jar_listing "$dir" | grep ' spring-boot' | awk '{s+=$1} END {print s}')"
        done
        echo
        echo "## In 3.5 only (by artifact name, version stripped)"
        comm -23 <(jar_listing "$a" | awk '{print $2}' | sed -E 's/-[0-9][^-]*(\.Final)?\.jar$//' | sort -u) \
                 <(jar_listing "$b" | awk '{print $2}' | sed -E 's/-[0-9][^-]*(\.Final)?\.jar$//' | sort -u)
        echo
        echo "## In 4.1 only"
        comm -13 <(jar_listing "$a" | awk '{print $2}' | sed -E 's/-[0-9][^-]*(\.Final)?\.jar$//' | sort -u) \
                 <(jar_listing "$b" | awk '{print $2}' | sed -E 's/-[0-9][^-]*(\.Final)?\.jar$//' | sort -u)
    } >"$out/summary.txt"
    release jars-35; release jars-41
}

case "${1:-all}" in
    step-0) step0 ;; step-1) step1 ;; step-2) step2 ;; step-3) step3 ;;
    step-4) step4 ;; step-5) step5 ;; step-6) step6 ;; jars) jars ;;
    all) step0; step1; step2; step3; step4; step5; step6; jars ;;
    *) echo "usage: $0 [all|step-0|step-1|step-2|step-3|step-4|step-5|step-6|jars]" >&2; exit 2 ;;
esac
log "done, transcripts in evidence/"
