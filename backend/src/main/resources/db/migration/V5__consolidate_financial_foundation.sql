-- Marco 1: consolidate tenant isolation, import lifecycle and private file retention.

do $$
declare
    legacy_table text;
    legacy_count bigint;
begin
    foreach legacy_table in array array['transactions', 'imports', 'category_rules', 'categories', 'accounts']
    loop
        if to_regclass(format('organizadorfinanceiro.%I', legacy_table)) is not null then
            execute format('select count(*) from organizadorfinanceiro.%I', legacy_table) into legacy_count;
            if legacy_count > 0 then
                raise exception 'Legacy table organizadorfinanceiro.% contains % rows; migrate them before Marco 1 consolidation',
                    legacy_table, legacy_count;
            end if;
        end if;
    end loop;
end
$$;

drop table if exists organizadorfinanceiro.transactions;
drop table if exists organizadorfinanceiro.imports;
drop table if exists organizadorfinanceiro.category_rules;
drop table if exists organizadorfinanceiro.categories;
drop table if exists organizadorfinanceiro.accounts;

alter table organizadorfinanceiro.import_file
    add column storage_bucket varchar(100),
    add column storage_path varchar(700),
    add column storage_status varchar(20) not null default 'STORED',
    add constraint import_file_storage_status_check
        check (storage_status in ('STORED', 'DELETED'));

alter table organizadorfinanceiro.import_execution
    add column target_type varchar(20),
    add column adapter_version varchar(40) not null default 'foundation-v1',
    add column rules_version varchar(40) not null default 'foundation-v1',
    add column created_at timestamptz not null default now();

alter table organizadorfinanceiro.import_execution
    drop constraint if exists import_execution_check;

alter table organizadorfinanceiro.import_execution
    add constraint import_execution_target_type_check
        check (target_type in ('ACCOUNT', 'CARD', 'PAYROLL')),
    add constraint import_execution_target_check
        check (
            (target_type = 'ACCOUNT' and account_id is not null and credit_card_id is null)
            or (target_type = 'CARD' and account_id is null and credit_card_id is not null)
            or (target_type = 'PAYROLL' and account_id is null and credit_card_id is null)
        );

alter table organizadorfinanceiro.import_file
    alter column storage_bucket set not null,
    alter column storage_path set not null,
    alter column storage_status drop default;

alter table organizadorfinanceiro.import_execution
    alter column target_type set not null,
    alter column adapter_version drop default,
    alter column rules_version drop default;

create table organizadorfinanceiro.import_warning (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    import_execution_id uuid not null,
    source_row_number integer,
    warning_code varchar(80) not null,
    warning_message varchar(500) not null,
    warning_context jsonb,
    created_at timestamptz not null default now(),
    check (source_row_number is null or source_row_number > 0)
);

-- Every user-owned row is anchored to Supabase Auth.
alter table organizadorfinanceiro.financial_institution
    add constraint financial_institution_user_fk foreign key (user_id) references auth.users(id) on delete cascade,
    add constraint financial_institution_user_id_uk unique (user_id, id);
alter table organizadorfinanceiro.financial_account
    add constraint financial_account_user_fk foreign key (user_id) references auth.users(id) on delete cascade,
    add constraint financial_account_user_id_uk unique (user_id, id);
alter table organizadorfinanceiro.credit_card
    add constraint credit_card_user_fk foreign key (user_id) references auth.users(id) on delete cascade,
    add constraint credit_card_user_id_uk unique (user_id, id);
alter table organizadorfinanceiro.import_file
    add constraint import_file_user_fk foreign key (user_id) references auth.users(id) on delete cascade,
    add constraint import_file_user_id_uk unique (user_id, id);
alter table organizadorfinanceiro.import_execution
    add constraint import_execution_user_fk foreign key (user_id) references auth.users(id) on delete cascade,
    add constraint import_execution_user_id_uk unique (user_id, id);
alter table organizadorfinanceiro.raw_transaction
    add constraint raw_transaction_user_fk foreign key (user_id) references auth.users(id) on delete cascade,
    add constraint raw_transaction_user_id_uk unique (user_id, id);
alter table organizadorfinanceiro.financial_transaction
    add constraint financial_transaction_user_fk foreign key (user_id) references auth.users(id) on delete cascade,
    add constraint financial_transaction_user_id_uk unique (user_id, id),
    add constraint financial_transaction_raw_uk unique (user_id, raw_transaction_id);
alter table organizadorfinanceiro.audit_event
    add constraint audit_event_user_fk foreign key (user_id) references auth.users(id) on delete cascade;
alter table organizadorfinanceiro.import_warning
    add constraint import_warning_user_fk foreign key (user_id) references auth.users(id) on delete cascade,
    add constraint import_warning_user_id_uk unique (user_id, id);

-- Composite foreign keys prevent a row from referencing another user's object.
alter table organizadorfinanceiro.financial_account
    drop constraint financial_account_institution_id_fkey,
    add constraint financial_account_institution_owner_fk
        foreign key (user_id, institution_id)
        references organizadorfinanceiro.financial_institution(user_id, id);

alter table organizadorfinanceiro.credit_card
    drop constraint credit_card_institution_id_fkey,
    drop constraint credit_card_payment_account_id_fkey,
    add constraint credit_card_institution_owner_fk
        foreign key (user_id, institution_id)
        references organizadorfinanceiro.financial_institution(user_id, id),
    add constraint credit_card_payment_account_owner_fk
        foreign key (user_id, payment_account_id)
        references organizadorfinanceiro.financial_account(user_id, id);

alter table organizadorfinanceiro.import_execution
    drop constraint import_execution_import_file_id_fkey,
    drop constraint import_execution_account_id_fkey,
    drop constraint import_execution_credit_card_id_fkey,
    add constraint import_execution_file_owner_fk
        foreign key (user_id, import_file_id)
        references organizadorfinanceiro.import_file(user_id, id),
    add constraint import_execution_account_owner_fk
        foreign key (user_id, account_id)
        references organizadorfinanceiro.financial_account(user_id, id),
    add constraint import_execution_card_owner_fk
        foreign key (user_id, credit_card_id)
        references organizadorfinanceiro.credit_card(user_id, id);

alter table organizadorfinanceiro.raw_transaction
    drop constraint raw_transaction_import_execution_id_fkey,
    add constraint raw_transaction_execution_owner_fk
        foreign key (user_id, import_execution_id)
        references organizadorfinanceiro.import_execution(user_id, id) on delete cascade;

alter table organizadorfinanceiro.financial_transaction
    drop constraint financial_transaction_raw_transaction_id_fkey,
    drop constraint financial_transaction_account_id_fkey,
    drop constraint financial_transaction_credit_card_id_fkey,
    add constraint financial_transaction_raw_owner_fk
        foreign key (user_id, raw_transaction_id)
        references organizadorfinanceiro.raw_transaction(user_id, id),
    add constraint financial_transaction_account_owner_fk
        foreign key (user_id, account_id)
        references organizadorfinanceiro.financial_account(user_id, id),
    add constraint financial_transaction_card_owner_fk
        foreign key (user_id, credit_card_id)
        references organizadorfinanceiro.credit_card(user_id, id);

alter table organizadorfinanceiro.import_warning
    add constraint import_warning_execution_owner_fk
        foreign key (user_id, import_execution_id)
        references organizadorfinanceiro.import_execution(user_id, id) on delete cascade;

create index financial_account_institution_owner_idx
    on organizadorfinanceiro.financial_account(user_id, institution_id);
create index credit_card_institution_owner_idx
    on organizadorfinanceiro.credit_card(user_id, institution_id);
create index credit_card_payment_account_owner_idx
    on organizadorfinanceiro.credit_card(user_id, payment_account_id) where payment_account_id is not null;
create index import_execution_file_owner_idx
    on organizadorfinanceiro.import_execution(user_id, import_file_id);
create index import_execution_account_owner_idx
    on organizadorfinanceiro.import_execution(user_id, account_id) where account_id is not null;
create index import_execution_card_owner_idx
    on organizadorfinanceiro.import_execution(user_id, credit_card_id) where credit_card_id is not null;
create index raw_transaction_execution_owner_idx
    on organizadorfinanceiro.raw_transaction(user_id, import_execution_id);
create index financial_transaction_raw_owner_idx
    on organizadorfinanceiro.financial_transaction(user_id, raw_transaction_id);
create index financial_transaction_account_owner_idx
    on organizadorfinanceiro.financial_transaction(user_id, account_id) where account_id is not null;
create index financial_transaction_card_owner_idx
    on organizadorfinanceiro.financial_transaction(user_id, credit_card_id) where credit_card_id is not null;
create index import_warning_execution_owner_idx
    on organizadorfinanceiro.import_warning(user_id, import_execution_id);
create index audit_event_user_idx on organizadorfinanceiro.audit_event(user_id);

alter table organizadorfinanceiro.import_warning enable row level security;

-- The browser authenticates with Supabase but all financial access goes through Spring.
revoke all on all tables in schema organizadorfinanceiro from anon, authenticated;
revoke usage on schema organizadorfinanceiro from anon, authenticated;

drop policy if exists financial_institution_owner on organizadorfinanceiro.financial_institution;
drop policy if exists financial_account_owner on organizadorfinanceiro.financial_account;
drop policy if exists credit_card_owner on organizadorfinanceiro.credit_card;
drop policy if exists import_file_owner on organizadorfinanceiro.import_file;
drop policy if exists import_execution_owner on organizadorfinanceiro.import_execution;
drop policy if exists raw_transaction_owner on organizadorfinanceiro.raw_transaction;
drop policy if exists financial_transaction_owner on organizadorfinanceiro.financial_transaction;
drop policy if exists audit_event_owner on organizadorfinanceiro.audit_event;

create policy financial_institution_owner_read on organizadorfinanceiro.financial_institution
    for select to authenticated using ((select auth.uid()) = user_id);
create policy financial_account_owner_read on organizadorfinanceiro.financial_account
    for select to authenticated using ((select auth.uid()) = user_id);
create policy credit_card_owner_read on organizadorfinanceiro.credit_card
    for select to authenticated using ((select auth.uid()) = user_id);
create policy import_file_owner_read on organizadorfinanceiro.import_file
    for select to authenticated using ((select auth.uid()) = user_id);
create policy import_execution_owner_read on organizadorfinanceiro.import_execution
    for select to authenticated using ((select auth.uid()) = user_id);
create policy raw_transaction_owner_read on organizadorfinanceiro.raw_transaction
    for select to authenticated using ((select auth.uid()) = user_id);
create policy financial_transaction_owner_read on organizadorfinanceiro.financial_transaction
    for select to authenticated using ((select auth.uid()) = user_id);
create policy audit_event_owner_read on organizadorfinanceiro.audit_event
    for select to authenticated using ((select auth.uid()) = user_id);
create policy import_warning_owner_read on organizadorfinanceiro.import_warning
    for select to authenticated using ((select auth.uid()) = user_id);

-- RAW and audit records are append-only, including for privileged application code.
create or replace function organizadorfinanceiro.reject_immutable_change()
returns trigger
language plpgsql
as $$
begin
    raise exception '% is append-only', tg_table_name;
end
$$;

create trigger raw_transaction_immutable
before update or delete on organizadorfinanceiro.raw_transaction
for each row execute function organizadorfinanceiro.reject_immutable_change();

create trigger audit_event_immutable
before update or delete on organizadorfinanceiro.audit_event
for each row execute function organizadorfinanceiro.reject_immutable_change();

alter table organizadorfinanceiro.flyway_schema_history enable row level security;
revoke all on organizadorfinanceiro.flyway_schema_history from anon, authenticated;

-- Supabase Storage is optional in local PostgreSQL; production has this schema.
do $$
begin
    if to_regclass('storage.buckets') is not null then
        insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
        values (
            'financial-imports',
            'financial-imports',
            false,
            10485760,
            array[
                'text/csv',
                'application/csv',
                'application/pdf',
                'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
                'application/octet-stream'
            ]
        )
        on conflict (id) do update set
            public = excluded.public,
            file_size_limit = excluded.file_size_limit,
            allowed_mime_types = excluded.allowed_mime_types;

        execute 'drop policy if exists financial_imports_insert on storage.objects';
        execute 'drop policy if exists financial_imports_select on storage.objects';
        execute 'drop policy if exists financial_imports_delete on storage.objects';

        execute $policy$
            create policy financial_imports_insert on storage.objects
            for insert to authenticated
            with check (
                bucket_id = 'financial-imports'
                and (storage.foldername(name))[1] = (select auth.uid())::text
            )
        $policy$;
        execute $policy$
            create policy financial_imports_select on storage.objects
            for select to authenticated
            using (
                bucket_id = 'financial-imports'
                and (storage.foldername(name))[1] = (select auth.uid())::text
            )
        $policy$;
        execute $policy$
            create policy financial_imports_delete on storage.objects
            for delete to authenticated
            using (
                bucket_id = 'financial-imports'
                and (storage.foldername(name))[1] = (select auth.uid())::text
            )
        $policy$;
    end if;
end
$$;
