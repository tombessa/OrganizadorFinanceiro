# Contratos dos importadores

## Pipeline comum

```text
detectar fonte -> validar formato -> hash -> extrair RAW -> normalizar
-> deduplicar -> interpretar -> conciliar -> classificar -> atribuir ciclo
```

O adaptador retorna avisos e erros estruturados. Uma execução só muda para `COMPLETED`
depois de persistir todas as linhas válidas e suas contagens em uma transação de banco.

## Conta corrente CSV

- UTF-8, separador `;`.
- O adaptador localiza o cabeçalho após metadados opcionais.
- Campos mínimos: data, descrição, valor e saldo.
- Arquivos com períodos sobrepostos são um caso obrigatório de teste de idempotência.

## Cartão CSV

- UTF-8 com BOM, CSV separado por vírgulas.
- Campos: `Data`, `Lançamento`, `Categoria`, `Tipo`, `Valor`.
- Compra positiva representa despesa; o sinal não define receita.
- Lançamentos de quitação devem ser candidatos a pagamento de fatura.
- O campo de tipo pode conter o número atual e o total de parcelas.

## Cartão XLSX

- Uma aba por competência; cabeçalho e lançamentos devem ser localizados por rótulo.
- Lançamentos: `Data`, `Lançamento`, `Parcelamento`, `Valor`.
- Data é serial numérico do Excel.
- Há cartões físicos, virtuais recorrentes e virtuais temporários, identificados pelos quatro últimos dígitos.
- Pagamentos da fatura não são novas despesas.
- O parcelamento informa parcela atual e total.

## Conta corrente PDF

- PDF textual com quantidade variável de páginas.
- Tabela: `Data`, `Descrição`, `Nº Documento`, `Movimento (R$)`, `Saldo (R$)`.
- O adaptador aceita débito sinalizado após o valor.
- Descrições podem continuar na linha seguinte.
- Saldo próprio, provisão de encargos e limite devem ser extraídos separadamente.

## Documento de remuneração PDF

- PDF textual com tabela de proventos e descontos.
- Campos: rubrica, descrição, mês/ref., quantidade, parcela, crédito e débito.
- Descrições podem ocupar mais de uma linha.
- O resumo possui data prevista, bruto, descontos e líquido.
- Classificação de rubrica é configurável: estrutural, extraordinária, tributo,
  previdência, saúde, dívida ou outra.
