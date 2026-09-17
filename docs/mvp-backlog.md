# Backlog por marcos

## Marco 1 - Fundação financeira

- [x] Arquitetura e contratos documentados.
- [x] Backend Java 21/Spring Boot 3.
- [x] Supabase Auth validado pelo Spring Security.
- [x] Flyway no schema `organizadorfinanceiro`.
- [x] Instituições, contas e cartões.
- [x] Metadados de arquivo, execução, RAW, transação e auditoria.
- [x] Docker Compose local e `Dockerfile.vercel`.
- [x] Frontend consumindo a API em vez das tabelas.
- [x] Bucket privado/volume local para retenção segura do arquivo.
- [x] Isolamento relacional por `user_id` e vínculo com `auth.users`.
- [x] Tabelas legadas vazias aposentadas com trava contra perda de dados.
- [x] Fila estrutural de avisos e versionamento de adaptador/regras.

Aceite: subir localmente, autenticar, cadastrar instituições/contas/cartões, armazenar o arquivo
com acesso privado e criar `ImportFile`, `ImportExecution` e auditoria sem iniciar o parser.

## Marco 2 - Importação real

Ordem: conta CSV, cartão CSV, cartão XLSX, conta PDF e documento de remuneração PDF.

- [x] Contrato real do extrato da conta Inter validado em UTF-8 e valores brasileiros.
- [x] Parser da conta Inter com conferência do saldo corrente e saldo final.
- [x] RAW imutável e transação normalizada persistidos na mesma execução.
- [x] Fingerprint por conta, data, descrição, valor e saldo após o lançamento.
- [x] Relatório de detectados, importados, duplicados e avisos na resposta e no frontend.
- [ ] Fatura do cartão Inter em CSV.
- [ ] Fatura do cartão Itaú em XLSX.
- [ ] Extrato da conta Santander em PDF.
- [ ] Contracheque em PDF.

Aceite: todos os arquivos históricos importam sem erro e a segunda importação não duplica lançamentos.

## Marco 3 - Inteligência financeira

Transferências próprias, pagamentos de fatura, estornos, reembolsos, parcelamentos,
recorrências, estabelecimento, categorias, confiança, auditoria e ciclo financeiro.

Aceite: transferências próprias e pagamentos de cartão não entram novamente como receita ou despesa.

## Marco 4 - Planejamento

Saldo Livre Real, reservas, renda estrutural, projeção, renda futura comprometida,
degraus de liberação e alertas.

Aceite: projetar eventual déficit antes da próxima remuneração e explicar os componentes do cálculo.
