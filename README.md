# Organizador Financeiro

Aplicação pessoal para importar extratos e faturas, classificar lançamentos, acompanhar fluxo de caixa e apoiar a reorganização financeira.

> **Privacidade:** comprovantes, extratos, faturas e credenciais bancárias não são versionados neste repositório. Use apenas arquivos locais durante a importação.

## Primeira entrega

- Importação local de CSV de banco e cartão.
- Normalização de lançamentos e prevenção de duplicidades.
- Categorias, regras de categorização e revisão manual.
- Visão mensal de receitas, despesas e saldo.
- Modelo de dados com isolamento por usuário (RLS no Supabase).

## Tecnologia

- React + Vite
- Supabase: PostgreSQL, autenticação e Row Level Security
- Hospedagem opcional: Vercel Hobby

## Executar localmente

```bash
cp .env.example .env.local
npm install
npm run dev
```

Execute antes `sql/0001_initial_schema.sql` no SQL Editor do projeto Supabase. Depois configure as variáveis no `.env.local`.

## Armazenamento recomendado

A primeira opção é **Supabase Free**: oferece PostgreSQL e autenticação, e o esquema deste repositório já usa RLS para impedir que um usuário consulte os dados de outro. O plano gratuito tem limites e projetos sem uso podem ser pausados; mantenha um export mensal como backup. A hospedagem no **Vercel Hobby** serve para uso pessoal/não comercial. Para uma opção totalmente local, use IndexedDB/SQLite — máxima privacidade, mas sem sincronização automática entre dispositivos.

## Próximos marcos

1. Conectar a importação aos formatos Santander e Inter reais, mantendo os arquivos fora do Git.
2. Tela de revisão de categorias e conciliação cartão x conta.
3. Orçamento, calendário de vencimentos e painel de saúde financeira.
4. Backup/exportação CSV e autenticação.