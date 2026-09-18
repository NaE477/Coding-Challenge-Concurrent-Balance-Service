alter table accounts
    add column deleted boolean not null default false;
