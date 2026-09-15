create schema if not exists organizadorfinanceiro;

create table if not exists organizadorfinanceiro.financial_institution (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    code varchar(40) not null,
    name varchar(120) not null,
    active boolean not null default true,
    created_at timestamptz not null default now(),
    unique (user_id, code)
);

create table if not exists organizadorfinanceiro.financial_account (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    institution_id uuid not null references organizadorfinanceiro.financial_institution(id),
    name varchar(120) not null,
    account_type varchar(30) not null check (account_type in ('CHECKING', 'CASH', 'INVESTMENT')),
    currency char(3) not null default 'BRL',
    overdraft_limit numeric(14,2) not null default 0 check (overdraft_limit >= 0),
    active boolean not null default true,
    created_at timestamptz not null default now(),
    unique (user_id, institution_id, name)
);

create table if not exists organizadorfinanceiro.credit_card (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    institution_id uuid not null references organizadorfinanceiro.financial_institution(id),
    payment_account_id uuid references organizadorfinanceiro.financial_account(id),
    name varchar(120) not null,
    last_four_digits char(4) not null check (last_four_digits ~ '^[0-9]{4}$'),
    closing_day smallint not null check (closing_day between 1 and 31),
    due_day smallint not null check (due_day between 1 and 31),
    active boolean not null default true,
    created_at timestamptz not null default now(),
    unique (user_id, institution_id, last_four_digits)
);

create table if not exists organizadorfinanceiro.import_file (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    source_adapter varchar(60) not null,
    original_filename varchar(255) not null,
    content_type varchar(120) not null,
    byte_size bigint not null check (byte_size > 0),
    content_hash char(64) not null check (content_hash ~ '^[0-9a-f]{64}$'),
    received_at timestamptz not null default now(),
    created_at timestamptz not null default now(),
    unique (user_id, source_adapter, content_hash)
);

create table if not exists organizadorfinanceiro.import_execution (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    import_file_id uuid not null references organizadorfinanceiro.import_file(id),
    account_id uuid references organizadorfinanceiro.financial_account(id),
    credit_card_id uuid references organizadorfinanceiro.credit_card(id),
    status varchar(30) not null check (status in ('RECEIVED', 'PARSING', 'COMPLETED', 'COMPLETED_WITH_WARNINGS', 'FAILED', 'DUPLICATE')),
    detected_rows integer not null default 0 check (detected_rows >= 0),
    imported_rows integer not null default 0 check (imported_rows >= 0),
    duplicate_rows integer not null default 0 check (duplicate_rows >= 0),
    warning_count integer not null default 0 check (warning_count >= 0),
    error_code varchar(80),
    error_summary varchar(500),
    started_at timestamptz not null default now(),
    finished_at timestamptz,
    check (account_id is not null or credit_card_id is not null)
);

create table if not exists organizadorfinanceiro.raw_transaction (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    import_execution_id uuid not null references organizadorfinanceiro.import_execution(id) on delete cascade,
    source_row_number integer not null check (source_row_number > 0),
    source_reference varchar(255),
    raw_date varchar(80),
    raw_description text,
    raw_amount varchar(80),
    raw_payload jsonb not null,
    raw_fingerprint char(64) not null,
    created_at timestamptz not null default now(),
    unique (import_execution_id, source_row_number),
    unique (user_id, raw_fingerprint)
);

create table if not exists organizadorfinanceiro.financial_transaction (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    raw_transaction_id uuid not null references organizadorfinanceiro.raw_transaction(id),
    account_id uuid references organizadorfinanceiro.financial_account(id),
    credit_card_id uuid references organizadorfinanceiro.credit_card(id),
    transaction_date date not null,
    posting_date date,
    amount numeric(14,2) not null check (amount >= 0),
    currency char(3) not null default 'BRL',
    direction varchar(10) not null check (direction in ('DEBIT', 'CREDIT')),
    transaction_type varchar(40) not null check (transaction_type in (
        'EXPENSE', 'INCOME', 'TRANSFER', 'CREDIT_CARD_PAYMENT', 'REFUND',
        'REIMBURSEMENT', 'SALARY', 'EXTRAORDINARY_INCOME', 'LOAN_PAYMENT',
        'LOAN_RECEIVABLE', 'INVESTMENT', 'PENSION', 'TAX', 'CASHBACK', 'ADJUSTMENT'
    )),
    raw_description text not null,
    normalized_description text,
    source varchar(60) not null,
    source_reference varchar(255),
    transaction_fingerprint char(64) not null,
    classification_confidence numeric(5,4),
    is_transfer boolean not null default false,
    is_reimbursement boolean not null default false,
    is_statement_payment boolean not null default false,
    is_recurring boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check ((account_id is not null) <> (credit_card_id is not null)),
    unique (user_id, transaction_fingerprint)
);

create table if not exists organizadorfinanceiro.audit_event (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    entity_type varchar(80) not null,
    entity_id uuid not null,
    action varchar(80) not null,
    actor_type varchar(20) not null check (actor_type in ('USER', 'SYSTEM')),
    reason varchar(500),
    confidence numeric(5,4),
    before_value jsonb,
    after_value jsonb,
    created_at timestamptz not null default now()
);

create index if not exists financial_account_user_idx
    on organizadorfinanceiro.financial_account(user_id);
create index if not exists credit_card_user_idx
    on organizadorfinanceiro.credit_card(user_id);
create index if not exists import_execution_user_started_idx
    on organizadorfinanceiro.import_execution(user_id, started_at desc);
create index if not exists raw_transaction_user_idx
    on organizadorfinanceiro.raw_transaction(user_id);
create index if not exists financial_transaction_user_date_idx
    on organizadorfinanceiro.financial_transaction(user_id, transaction_date desc);
create index if not exists audit_event_entity_idx
    on organizadorfinanceiro.audit_event(user_id, entity_type, entity_id, created_at desc);

alter table organizadorfinanceiro.financial_institution enable row level security;
alter table organizadorfinanceiro.financial_account enable row level security;
alter table organizadorfinanceiro.credit_card enable row level security;
alter table organizadorfinanceiro.import_file enable row level security;
alter table organizadorfinanceiro.import_execution enable row level security;
alter table organizadorfinanceiro.raw_transaction enable row level security;
alter table organizadorfinanceiro.financial_transaction enable row level security;
alter table organizadorfinanceiro.audit_event enable row level security;

create policy financial_institution_owner on organizadorfinanceiro.financial_institution
    for all to authenticated using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy financial_account_owner on organizadorfinanceiro.financial_account
    for all to authenticated using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy credit_card_owner on organizadorfinanceiro.credit_card
    for all to authenticated using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy import_file_owner on organizadorfinanceiro.import_file
    for all to authenticated using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy import_execution_owner on organizadorfinanceiro.import_execution
    for all to authenticated using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy raw_transaction_owner on organizadorfinanceiro.raw_transaction
    for all to authenticated using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy financial_transaction_owner on organizadorfinanceiro.financial_transaction
    for all to authenticated using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy audit_event_owner on organizadorfinanceiro.audit_event
    for select to authenticated using (auth.uid() = user_id);

revoke all on all tables in schema organizadorfinanceiro from anon;
grant usage on schema organizadorfinanceiro to authenticated;
grant select, insert, update, delete on all tables in schema organizadorfinanceiro to authenticated;
grant select on organizadorfinanceiro.audit_event to authenticated;
revoke insert, update, delete on organizadorfinanceiro.audit_event from authenticated;
