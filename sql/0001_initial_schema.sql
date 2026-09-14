-- Execute no SQL Editor do Supabase.
create extension if not exists pgcrypto;

create table public.categories (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  kind text not null check (kind in ('income', 'expense', 'transfer')),
  color text not null default '#1c6a3d',
  created_at timestamptz not null default now(),
  unique (user_id, name)
);

create table public.accounts (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  institution text,
  type text not null check (type in ('checking', 'credit_card', 'cash', 'investment')),
  active boolean not null default true,
  created_at timestamptz not null default now()
);

create table public.imports (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  account_id uuid references public.accounts(id) on delete set null,
  source text not null,
  original_filename text not null,
  imported_at timestamptz not null default now()
);

create table public.transactions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  account_id uuid not null references public.accounts(id) on delete cascade,
  import_id uuid references public.imports(id) on delete set null,
  category_id uuid references public.categories(id) on delete set null,
  transaction_date date not null,
  description text not null,
  amount numeric(14,2) not null check (amount <> 0),
  transaction_hash text not null,
  status text not null default 'review' check (status in ('review', 'confirmed', 'ignored')),
  notes text,
  created_at timestamptz not null default now(),
  unique (user_id, account_id, transaction_hash)
);

create table public.category_rules (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  category_id uuid not null references public.categories(id) on delete cascade,
  pattern text not null,
  priority smallint not null default 100,
  created_at timestamptz not null default now()
);

alter table public.categories enable row level security;
alter table public.accounts enable row level security;
alter table public.imports enable row level security;
alter table public.transactions enable row level security;
alter table public.category_rules enable row level security;

create policy "users manage own categories" on public.categories for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "users manage own accounts" on public.accounts for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "users manage own imports" on public.imports for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "users manage own transactions" on public.transactions for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "users manage own category rules" on public.category_rules for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

create index transactions_user_date_idx on public.transactions (user_id, transaction_date desc);
create index category_rules_user_priority_idx on public.category_rules (user_id, priority);
