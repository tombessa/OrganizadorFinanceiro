-- Migração para o schema dedicado do projeto.
-- Pré-requisito: a migração 0001 já foi executada e as tabelas estão em public.
-- Não execute 0001 novamente após esta migração.

create schema if not exists organizadorfinanceiro;

alter table public.categories set schema organizadorfinanceiro;
alter table public.accounts set schema organizadorfinanceiro;
alter table public.imports set schema organizadorfinanceiro;
alter table public.transactions set schema organizadorfinanceiro;
alter table public.category_rules set schema organizadorfinanceiro;

grant usage on schema organizadorfinanceiro to anon, authenticated, service_role;
grant select, insert, update, delete on all tables in schema organizadorfinanceiro to anon, authenticated, service_role;
alter default privileges in schema organizadorfinanceiro
  grant select, insert, update, delete on tables to anon, authenticated, service_role;
