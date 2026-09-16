# Arquitetura do Organizador Financeiro

## Decisões vinculantes

- Monólito modular no MVP; nenhum microserviço.
- Frontend React + TypeScript hospedado no Vercel.
- Backend Java 21 + Spring Boot 3 empacotado como imagem OCI em Vercel Functions.
- PostgreSQL do Supabase Free, schema `organizadorfinanceiro`.
- Supabase Auth; a API valida o JWT e usa o `sub` como `user_id`.
- Arquivos financeiros são retidos em armazenamento privado, segregado por `user_id`, para auditoria e reprocessamento.
- O conteúdo bruto extraído e o hash do arquivo são preservados no banco para auditoria e reprocessamento.
- O navegador não grava diretamente nas tabelas financeiras.

## Topologia

```text
React/Vercel -> Bearer JWT -> Spring Boot/Vercel Function -> Supabase PostgreSQL
       |                              |                    -> Supabase Storage privado
Supabase Auth                 adaptadores de importação
```

## Módulos

`auth`, `users`, `institutions`, `accounts`, `cards`, `imports`, `transactions`,
`classification`, `reconciliation`, `recurrence`, `installments`, `payroll`,
`loans`, `cashflow`, `planning`, `health`, `alerts`, `dashboard` e `audit`.

Os módulos compartilham uma aplicação e um banco, mas não acessam entidades internas de outro módulo diretamente. A comunicação ocorre por serviços públicos do módulo e identificadores.

## Segurança

- HTTPS obrigatório em produção.
- O frontend recebe apenas a chave pública `anon` do Supabase.
- A API valida assinatura, emissor e validade do JWT.
- Toda consulta de negócio inclui o `user_id` obtido do token; nunca aceita `user_id` enviado pelo cliente.
- Nenhuma senha bancária, token de internet banking ou chave `service_role` no frontend ou backend.
- Uploads no Storage usam o JWT do usuário e a chave publicável já presente no frontend; RLS limita o caminho ao próprio `user_id`.
- Logs registram IDs técnicos, contagens e códigos de erro, sem conteúdo financeiro bruto.
- Importações e alterações de classificação geram eventos de auditoria.

## Serverless

O backend deve iniciar pelo `PORT` fornecido pelo Vercel. Não utiliza disco persistente,
sessão em memória, agendador local ou fila interna. Arquivos persistentes vão para o
Supabase Storage e os metadados/execuções terminam no PostgreSQL antes da resposta.
