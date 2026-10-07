create table orders (
    id             bigserial primary key,
    customer_name  varchar(200)   not null,
    total_amount   numeric(12, 2) not null,
    currency       char(3)        not null,
    placed_on      date           not null,
    payment_status varchar(20)    not null
);

create table order_lines (
    order_id   bigint         not null references orders (id),
    sku        varchar(64)    not null,
    quantity   integer        not null,
    unit_price numeric(12, 2) not null,
    currency   char(3)        not null
);
