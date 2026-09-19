# Modelo de dados - fundação

## Princípios

1. Separar arquivo, execução, registro bruto, transação normalizada e classificação.
2. Preservar o arquivo em bucket privado e o dado bruto extraído de forma imutável.
3. Nunca inferir natureza econômica somente pelo sinal do valor.
4. Limite bancário é crédito disponível, não saldo próprio.
5. Todas as entidades do usuário possuem `user_id`.

## Entidades do Marco 1

| Entidade | Finalidade |
|---|---|
| `financial_institution` | Instituições financeiras cadastradas pelo usuário |
| `financial_account` | Conta corrente, dinheiro ou investimento; separa saldo próprio e limite |
| `credit_card` | Cartão, fechamento, vencimento e final mascarado |
| `import_file` | Hash, metadados e referência ao objeto privado no Storage |
| `import_execution` | Estado, adaptador, contagens, erros e duração da importação |
| `raw_transaction` | Linha extraída e payload JSON original imutável |
| `financial_transaction` | Representação normalizada e natureza econômica |
| `audit_event` | Alterações automáticas e manuais com motivo e confiança |
| `import_warning` | Avisos explicáveis, vinculados à execução e à linha de origem |

Todas as entidades próprias do usuário referenciam `auth.users`. Relações entre instituições,
contas, cartões, arquivos, execuções, RAW e transações usam chaves compostas com `user_id`,
impedindo referências cruzadas entre usuários mesmo em caso de falha na camada de aplicação.

## Próximas migrations

- Classificação: `merchant`, `category`, `classification_rule`.
- Conciliação: `financial_transfer`, `reimbursement`, `reconciliation_link`.
- Planejamento: `recurring_transaction`, `installment_purchase`, `installment`,
  `financial_cycle`, `reservation`.
- Folha e dívidas: `payroll`, `payroll_item`, `loan`, `loan_installment`, `third_party_loan`.
- Indicadores: `health_metric`, `alert`, `budget`, `financial_goal`.

## Idempotência

- Arquivo: SHA-256 dos bytes + usuário + adaptador.
- Registro bruto: execução + número lógico da linha.
- Transação: fingerprint específica do adaptador, usando os identificadores disponíveis.
- Duas operações legítimas com mesma data, descrição e valor não podem ser colapsadas sem evidência adicional.

### Extrato da conta Inter

O fingerprint inclui a conta selecionada, a data, a descrição canônica, o valor assinado e o
saldo após o lançamento. O saldo diferencia eventos legítimos iguais no mesmo dia e permanece
estável quando dois extratos possuem períodos sobrepostos. O sinal define somente `DEBIT` ou
`CREDIT`; a natureza econômica permanece `ADJUSTMENT` até o motor explicável do Marco 3.

### Fatura do cartão Inter

O fingerprint inclui cartão, data, descrição, categoria, tipo, valor assinado, posição da parcela e
uma ocorrência ordinal para preservar compras legítimas idênticas. Valores positivos no documento
viram `DEBIT` no cartão e valores negativos viram `CREDIT`; a natureza econômica continua
`ADJUSTMENT` até o Marco 3. `document_status` registra se a fonte é efetiva ou projetada, enquanto
`posting_status` impede que parcelas futuras sejam contabilizadas como despesas já realizadas.
