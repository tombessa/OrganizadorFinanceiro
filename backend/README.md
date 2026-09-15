# Backend

API modular em Java 21 e Spring Boot 3. O usuário é identificado pelo `sub` do JWT do Supabase.

## Ambiente local

Na raiz do repositório:

```bash
export LOCAL_POSTGRES_PASSWORD='defina-uma-senha-local'
docker compose up --build
```

Health check público: `GET http://localhost:8080/api/health`.

As demais rotas exigem `Authorization: Bearer <access_token>`:

- `GET|POST /api/institutions`
- `GET|POST /api/accounts`
- `GET|POST /api/cards`
- `POST /api/imports/register` (`multipart/form-data` com `adapter` e `file`)

## Supabase

Use a conexão PostgreSQL/pooler do projeto e configure o emissor JWT como:

```text
https://<project-ref>.supabase.co/auth/v1
```

O Flyway usa `baseline-version=2` porque as migrations SQL 0001 e 0002 já foram executadas manualmente.

## Vercel

Crie um segundo projeto Vercel apontando para o mesmo repositório, configure `backend` como
Root Directory e selecione o `Dockerfile.vercel`. Mantenha o projeto atual da raiz para o frontend.
No backend, configure `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`,
`SUPABASE_JWT_ISSUER` e `CORS_ALLOWED_ORIGINS`.

O arquivo original enviado não é armazenado. A API persiste somente nome, tipo, tamanho,
SHA-256 e, nos próximos adaptadores, o conteúdo textual/estruturado extraído.
