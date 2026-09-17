# Backend

API modular em Java 21 e Spring Boot 3. O usuário é identificado pelo `sub` do JWT do Supabase.

## Ambiente local

Na raiz do repositório:

```bash
export LOCAL_POSTGRES_PASSWORD='defina-uma-senha-local'
docker compose up --build
```

Health checks públicos: `GET http://localhost:8080/` e `GET http://localhost:8080/api/health`.

As demais rotas exigem `Authorization: Bearer <access_token>`:

- `GET|POST /api/institutions`
- `GET|POST /api/accounts`
- `GET|POST /api/cards`
- `POST /api/imports/register` (`multipart/form-data` com `adapter`, `file` e `targetId` para conta/cartão)

`INTER_ACCOUNT_CSV` já executa o pipeline completo: valida metadados e saldos, grava RAW,
normaliza os lançamentos e informa quantidades detectadas, novas e duplicadas. Os demais
adaptadores permanecem no estado `RECEIVED` até seus incrementos do Marco 2.

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
`SUPABASE_JWT_ISSUER` e `CORS_ALLOWED_ORIGINS`. O Storage usa a URL derivada do emissor JWT,
o bucket privado `financial-imports` e o JWT do próprio usuário; nenhuma `service_role` é necessária.

`DATABASE_URL` deve começar com `jdbc:postgresql://`. O contêiner do Vercel escuta
na porta padrão `80`; se a variável `PORT` for definida no projeto, o Spring usará
o valor informado pelo Vercel.

Em produção, o arquivo original fica no bucket privado `financial-imports`, em caminho iniciado
pelo `user_id` e protegido por RLS. Em Docker Compose, ele fica no volume privado
`organizadorfinanceiro_imports`. A API persiste hash, localização, execução e auditoria.
