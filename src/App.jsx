import { useEffect, useMemo, useState } from "react";
import AuthPanel from "./components/AuthPanel.jsx";
import { normalizeRows } from "./lib/normalizers.js";
import { requireSupabase } from "./lib/supabase.js";
import { saveImport } from "./lib/financeRepository.js";

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
  const [session, setSession] = useState(null);
  const [accountName, setAccountName] = useState("Santander");
  const [accountType, setAccountType] = useState("checking");
  const [status, setStatus] = useState("");

  useEffect(() => {
    try {
      const client = requireSupabase();
      client.auth.getSession().then(({ data }) => setSession(data.session));
      const { data: listener } = client.auth.onAuthStateChange((_event, nextSession) => setSession(nextSession));
      return () => listener.subscription.unsubscribe();
    } catch { setStatus("Configure o Supabase em .env.local para salvar seus dados."); }
  }, []);

  const summary = useMemo(() => transactions.reduce((acc, item) => {
    if (item.amount >= 0) acc.income += item.amount; else acc.expenses += Math.abs(item.amount);
    return acc;
  }, { income: 0, expenses: 0 }), [transactions]);
  const balance = summary.income - summary.expenses;

  async function handleImport(event) {
    const file = event.target.files?.[0];
    if (!file) return;
    setFileName(file.name);
    setTransactions(normalizeRows(parseCsv(await file.text())));
    setStatus("");
  }

  function updateCategory(id, category) {
    setTransactions((items) => items.map((item) => item.id === id ? { ...item, category } : item));
  }

  async function persist() {
    if (!session) { setStatus("Entre antes de salvar os lançamentos."); return; }
    if (transactions.some((item) => !item.isoDate)) { setStatus("Há lançamentos com data inválida. Revise o arquivo antes de salvar."); return; }
    try {
      setStatus("Salvando...");
      const source = accountName.toLowerCase().includes("inter") ? "Inter" : "Santander";
      const result = await saveImport({ session, accountName, accountType, source, filename: fileName, transactions });
      setStatus(`${result.count} lançamento(s) processado(s). Duplicados foram ignorados.`);
    } catch (error) { setStatus(`Não foi possível salvar: ${error.message}`); }
  }

  return <main>
    <section className="topbar"><span className="eyebrow">ORGANIZADOR FINANCEIRO</span><AuthPanel session={session} onSessionChange={setSession} /></section>
    <section className="hero"><div><h1>Entenda seu dinheiro antes de tentar consertá-lo.</h1><p>Importe um CSV do Santander ou Inter, revise e salve em sua conta protegida.</p></div>
      <label className="upload">Importar CSV<input type="file" accept=".csv,text/csv" onChange={handleImport} /></label></section>
    <section className="cards"><article><span>Receitas</span><strong>R$ {summary.income.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}</strong></article><article><span>Despesas</span><strong>R$ {summary.expenses.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}</strong></article><article className={balance < 0 ? "negative" : ""}><span>Resultado</span><strong>R$ {balance.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}</strong></article></section>
    <section className="tableCard"><header><div><h2>Lançamentos importados</h2><p>{fileName ? `${fileName} · ${transactions.length} itens` : "Nenhum arquivo selecionado"}</p></div><button onClick={() => setTransactions([])}>Limpar</button></header>
      {transactions.length === 0 ? <p className="empty">Comece com um extrato CSV. Nenhum dado é salvo sem seu comando.</p> : <><div className="savebar"><input value={accountName} onChange={(e) => setAccountName(e.target.value)} placeholder="Conta ou cartão" /><select value={accountType} onChange={(e) => setAccountType(e.target.value)}><option value="checking">Conta corrente</option><option value="credit_card">Cartão de crédito</option></select><button className="save" onClick={persist}>Salvar lançamentos</button></div><table><thead><tr><th>Data</th><th>Descrição</th><th>Categoria</th><th>Valor</th></tr></thead><tbody>{transactions.slice(0,100).map((item) => <tr key={item.id}><td>{item.date}</td><td>{item.description}</td><td><select value={item.category} onChange={(e) => updateCategory(item.id,e.target.value)}><option>A revisar</option><option>Moradia</option><option>Alimentação</option><option>Comunicação</option><option>Transporte</option><option>Saúde</option></select></td><td className={item.amount < 0 ? "expense" : "income"}>{item.amount.toLocaleString("pt-BR", { style:"currency",currency:"BRL" })}</td></tr>)}</tbody></table></>}
      {status && <p className="status">{status}</p>}
    </section>
  </main>;
}
