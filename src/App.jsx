import { useMemo, useState } from "react";
import { normalizeRows } from "./lib/normalizers.js";

function parseCsv(text) {
  const lines = text.replace(/^\uFEFF/, "").trim().split(/\r?\n/);
  const separator = lines[0]?.includes(";") ? ";" : ",";
  const headers = lines.shift().split(separator).map((item) => item.trim().replace(/^"|"$/g, ""));
  return lines.filter(Boolean).map((line) => {
    const cells = line.split(separator).map((item) => item.trim().replace(/^"|"$/g, ""));
    return Object.fromEntries(headers.map((header, index) => [header, cells[index] ?? ""]));
  });
}

export default function App() {
  const [transactions, setTransactions] = useState([]);
  const [fileName, setFileName] = useState("");
  const summary = useMemo(() => transactions.reduce((acc, item) => {
    if (item.amount >= 0) acc.income += item.amount;
    else acc.expenses += Math.abs(item.amount);
    return acc;
  }, { income: 0, expenses: 0 }), [transactions]);
  const balance = summary.income - summary.expenses;

  async function handleImport(event) {
    const file = event.target.files?.[0];
    if (!file) return;
    const text = await file.text();
    setFileName(file.name);
    setTransactions(normalizeRows(parseCsv(text)));
  }

  return <main>
    <section className="hero">
      <div><p className="eyebrow">ORGANIZADOR FINANCEIRO</p><h1>Entenda seu dinheiro antes de tentar consertá-lo.</h1>
      <p>Importe um CSV do Santander ou Inter. Nesta etapa, o arquivo é processado somente no seu navegador.</p></div>
      <label className="upload">Importar CSV<input type="file" accept=".csv,text/csv" onChange={handleImport} /></label>
    </section>

    <section className="cards">
      <article><span>Receitas</span><strong>R$ {summary.income.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}</strong></article>
      <article><span>Despesas</span><strong>R$ {summary.expenses.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}</strong></article>
      <article className={balance < 0 ? "negative" : ""}><span>Resultado</span><strong>R$ {balance.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}</strong></article>
    </section>

    <section className="tableCard">
      <header><div><h2>Lançamentos importados</h2><p>{fileName ? `${fileName} · ${transactions.length} itens` : "Nenhum arquivo selecionado"}</p></div><button onClick={() => setTransactions([])}>Limpar</button></header>
      {transactions.length === 0 ? <p className="empty">Comece com um extrato CSV. Em seguida, você poderá revisar as categorias antes de salvar.</p> :
      <table><thead><tr><th>Data</th><th>Descrição</th><th>Categoria sugerida</th><th>Valor</th></tr></thead>
      <tbody>{transactions.slice(0, 100).map((item) => <tr key={item.id}><td>{item.date}</td><td>{item.description}</td><td><select defaultValue={item.category}><option>A revisar</option><option>Moradia</option><option>Alimentação</option><option>Comunicação</option><option>Transporte</option><option>Saúde</option></select></td><td className={item.amount < 0 ? "expense" : "income"}>{item.amount.toLocaleString("pt-BR", { style: "currency", currency: "BRL" })}</td></tr>)}</tbody></table>}
    </section>
  </main>;
}
