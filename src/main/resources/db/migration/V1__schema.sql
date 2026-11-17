create extension if not exists btree_gist;

create table users (
    id            uuid primary key,
    email         varchar(254) not null unique,
    name          varchar(80)  not null,
    password_hash varchar(100) not null,
    role          varchar(10)  not null check (role in ('USER', 'ADMIN')),
    created_at    timestamptz  not null
);

create table spaces (
    id          uuid primary key,
    name        varchar(80)  not null,
    description varchar(500) not null default '',
    capacity    integer      not null check (capacity between 1 and 500),
    time_zone   varchar(64)  not null,
    opens_at    time         not null,
    closes_at   time         not null,
    active      boolean      not null default true,
    created_at  timestamptz  not null,
    check (opens_at < closes_at)
);

create unique index spaces_name_idx on spaces (lower(name));

create table bookings (
    id           uuid primary key,
    space_id     uuid         not null references spaces (id),
    user_id      uuid         not null references users (id) on delete cascade,
    title        varchar(120) not null,
    starts_at    timestamptz  not null,
    ends_at      timestamptz  not null,
    status       varchar(10)  not null check (status in ('CONFIRMED', 'CANCELLED')),
    created_at   timestamptz  not null,
    cancelled_at timestamptz,
    check (starts_at < ends_at),
    constraint bookings_no_overlap exclude using gist (
        space_id with =,
        tstzrange(starts_at, ends_at, '[)') with &&
    ) where (status = 'CONFIRMED')
);

create index bookings_user_idx on bookings (user_id, starts_at);
