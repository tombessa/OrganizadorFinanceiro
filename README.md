# Organizador Financeiro

Aplicação pessoal para importar extratos e faturas, classificar lançamentos, acompanhar fluxo de caixa e apoiar a reorganização financeira.

> **Privacidade:** comprovantes, extratos, faturas e credenciais bancárias não são versionados neste repositório. Use apenas arquivos locais durante a importação.

## Configuração concluída

O banco usa o schema PostgreSQL `organizadorfinanceiro` no Supabase.

Como a primeira migração já foi executada com as tabelas em `public`, execute agora `sql/0002_move_to_organizadorfinanceiro_schema.sql` no SQL Editor. Ela move as tabelas sem apagar os dados ou as políticas de RLS.

No painel do Supabase, acrescente `organizadorfinanceiro` em **Project Settings → API → Exposed schemas**. Isso permite que o cliente use `supabase.schema("organizadorfinanceiro")`; não exponha o schema `auth`.

## Executar localmente

```bash
npm install
npm run dev
```

O arquivo `.env.local` deve conter:

```env
VITE_SUPABASE_URL=https://SEU-PROJETO.supabase.co
VITE_SUPABASE_ANON_KEY=SUA_CHAVE_PUBLICA
```

## Primeira entrega

- Importação local de CSV de banco e cartão.
- Normalização de lançamentos e prevenção de duplicidades.
- Categorias, regras de categorização e revisão manual.
- Visão mensal de receitas, despesas e saldo.
- Modelo de dados com isolamento por usuário (RLS no Supabase).

## Próximos marcos

1. Habilitar autenticação e criar a conta inicial.
2. Conectar o importador aos formatos Santander e Inter, com persistência e deduplicação.
3. Tela de revisão de categorias e conciliação cartão x conta.
4. Orçamento, calendário de vencimentos e painel de saúde financeira.
5. Exportação para backup.

## Armazenamento

Supabase Free armazena dados e autenticação; faça uma exportação mensal como backup. A hospedagem no Vercel Hobby é adequada para uso pessoal/não comercial.
