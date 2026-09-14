-- Execute no SQL Editor do Supabase.
create extension if not exists pgcrypto;

create table organizadorfinanceiro.categories (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  kind text not null check (kind in ('income', 'expense', 'transfer')),
  color text not null default '#1c6a3d',
  created_at timestamptz not null default now(),
  unique (user_id, name)
);

create table organizadorfinanceiro.accounts (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  institution text,
  type text not null check (type in ('checking', 'credit_card', 'cash', 'investment')),
  active boolean not null default true,
  created_at timestamptz not null default now()
);

create table organizadorfinanceiro.imports (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  account_id uuid references organizadorfinanceiro.accounts(id) on delete set null,
  source text not null,
  original_filename text not null,
  imported_at timestamptz not null default now()
);

create table organizadorfinanceiro.transactions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  account_id uuid not null references organizadorfinanceiro.accounts(id) on delete cascade,
  import_id uuid references organizadorfinanceiro.imports(id) on delete set null,
  category_id uuid references organizadorfinanceiro.categories(id) on delete set null,
  transaction_date date not null,
  description text not null,
  amount numeric(14,2) not null check (amount <> 0),
  transaction_hash text not null,
  status text not null default 'review' check (status in ('review', 'confirmed', 'ignored')),
  notes text,
  created_at timestamptz not null default now(),
  unique (user_id, account_id, transaction_hash)
);

create table organizadorfinanceiro.category_rules (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  category_id uuid not null references organizadorfinanceiro.categories(id) on delete cascade,
  pattern text not null,
  priority smallint not null default 100,
  created_at timestamptz not null default now()
);

alter table organizadorfinanceiro.categories enable row level security;
alter table organizadorfinanceiro.accounts enable row level security;
alter table organizadorfinanceiro.imports enable row level security;
alter table organizadorfinanceiro.transactions enable row level security;
alter table organizadorfinanceiro.category_rules enable row level security;

create policy "users manage own categories" on organizadorfinanceiro.categories for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "users manage own accounts" on organizadorfinanceiro.accounts for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "users manage own imports" on organizadorfinanceiro.imports for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "users manage own transactions" on organizadorfinanceiro.transactions for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "users manage own category rules" on organizadorfinanceiro.category_rules for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

create index transactions_user_date_idx on organizadorfinanceiro.transactions (user_id, transaction_date desc);
create index category_rules_user_priority_idx on organizadorfinanceiro.category_rules (user_id, priority);
