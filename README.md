# Organizador Financeiro

Aplicação pessoal para centralizar contas, cartões e importações financeiras com isolamento por usuário.

Frontend em produção: <https://organizador-financeiro-one.vercel.app/>

## Arquitetura

- **Frontend:** React, TypeScript e Vite, publicado na Vercel.
- **Autenticação:** Supabase Auth.
- **API:** Spring Boot, com validação dos JWTs emitidos pelo Supabase.
- **Dados:** PostgreSQL do Supabase, no schema `organizadorfinanceiro`.
- **Documentos:** o backend calcula o SHA-256, mantém o arquivo em bucket privado segregado por usuário e registra uma execução auditável.

O frontend não grava diretamente nas tabelas financeiras. Depois da autenticação, ele envia o access token para a API Spring usando o cabeçalho `Authorization: Bearer`.

## Executar o frontend

1. Copie `.env.example` para `.env.local`.
2. Preencha as variáveis públicas do projeto Supabase e a URL da API.
3. Execute:

```bash
npm install
npm run dev
```

Para validar a compilação de produção:

```bash
npm run build
```

## Variáveis do frontend na Vercel

Configure estas variáveis para Production, Preview e Development:

```dotenv
VITE_SUPABASE_URL=https://seu-projeto.supabase.co
VITE_SUPABASE_ANON_KEY=sua-chave-publica-anon
VITE_API_URL=https://organizador-financeiro-backend.vercel.app
```

Depois de alterar variáveis na Vercel, faça um novo deploy para incorporá-las ao bundle do Vite.

## Backend

As instruções específicas da API estão em [`backend/README.md`](backend/README.md). As migrations do backend são gerenciadas pelo Flyway em `backend/src/main/resources/db/migration`.
