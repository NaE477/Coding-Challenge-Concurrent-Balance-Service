create table accounts
(
    id      varchar(64) primary key,
    balance bigint      not null check (balance >= 0)
);

create table transactions
(
    transaction_id         varchar(64) primary key,
    type                   varchar(16) not null,
    source_account_id      varchar(64),
    destination_account_id varchar(64),
    amount                 bigint      not null check (amount > 0),
    created_at             timestamptz not null default now()
);
