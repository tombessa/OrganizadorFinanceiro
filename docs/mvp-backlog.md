# Backlog por marcos

## Marco 1 - Fundação financeira

- [x] Arquitetura e contratos documentados.
- [x] Backend Java 21/Spring Boot 3.
- [x] Supabase Auth validado pelo Spring Security.
- [x] Flyway no schema `organizadorfinanceiro`.
- [x] Instituições, contas e cartões.
- [x] Metadados de arquivo, execução, RAW, transação e auditoria.
- [x] Docker Compose local e `Dockerfile.vercel`.
- [ ] Frontend consumindo a API em vez das tabelas.

Aceite: subir localmente, autenticar, cadastrar instituições/contas/cartões e registrar com segurança os metadados de uma importação.

## Marco 2 - Importação real

Ordem: conta CSV, cartão CSV, cartão XLSX, conta PDF e documento de remuneração PDF.

Aceite: todos os arquivos históricos importam sem erro e a segunda importação não duplica lançamentos.

## Marco 3 - Inteligência financeira

Transferências próprias, pagamentos de fatura, estornos, reembolsos, parcelamentos,
recorrências, estabelecimento, categorias, confiança, auditoria e ciclo financeiro.

Aceite: transferências próprias e pagamentos de cartão não entram novamente como receita ou despesa.

## Marco 4 - Planejamento

Saldo Livre Real, reservas, renda estrutural, projeção, renda futura comprometida,
degraus de liberação e alertas.

Aceite: projetar eventual déficit antes da próxima remuneração e explicar os componentes do cálculo.
