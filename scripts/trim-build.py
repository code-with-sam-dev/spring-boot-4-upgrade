"""Keep the lines of a Maven log that tell the story, drop the noise.

usage: python3 trim-build.py <maven.log> <path prefix to strip>...
"""
import re
import sys

log_path, prefixes = sys.argv[1], sys.argv[2:]
lines = open(log_path, encoding="utf-8", errors="replace").read().splitlines()

keep = []
in_goal_failure = False
after_test_failure = 0
seen = set()


def add(line, dedupe=True):
    for p in prefixes:
        line = line.replace(p.rstrip("/") + "/", "")
    if not dedupe or line not in seen or line.strip() == "":
        seen.add(line)
        keep.append(line)


for line in lines:
    if in_goal_failure:
        if "-> [Help" in line or line.startswith("[ERROR] Re-run") or line.startswith("[ERROR] To see"):
            in_goal_failure = False
            continue
        add(line, dedupe=False)
        continue
    if after_test_failure:
        after_test_failure -= 1
        if line.strip() and not line.lstrip().startswith("at "):
            add("    " + line.strip())
        continue
    if re.match(r"\[ERROR\] Failed to execute goal .*(compile|testCompile)", line):
        add(line)
        in_goal_failure = True
    elif "dependencies.dependency.version" in line and "@ line" in line:
        add(re.sub(r"^\[ERROR\]\s+", "[ERROR] ", line))
    elif re.match(r"\[WARNING\] /.*\.java", line):
        add(line)
    elif re.search(r"Tests run: \d+, Failures: \d+, Errors: \d+, Skipped: \d+", line):
        add(line)
    elif "<<< FAILURE!" in line or "<<< ERROR!" in line:
        if " -- in " not in line:
            add(line)
            after_test_failure = 2
    elif re.match(r"\[ERROR\]   \S+(IT|Test)\.\S+", line):
        add(line)
    elif re.search(r"BUILD (SUCCESS|FAILURE)", line):
        add(line)

print("\n".join(keep))
