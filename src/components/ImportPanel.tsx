import { useEffect, useMemo, useState, type FormEvent } from "react";
import { apiRequest } from "../lib/api";
import type { CreditCard, FinancialAccount, ImportRegistration, SourceAdapter } from "../types";

interface ImportPanelProps { accessToken: string; }

const adapterOptions: Array<{ value: SourceAdapter; label: string; accept: string }> = [
  { value: "INTER_ACCOUNT_CSV", label: "Inter — Conta (CSV)", accept: ".csv,text/csv" },
  { value: "INTER_CARD_CSV", label: "Inter — Cartão (CSV)", accept: ".csv,text/csv" },
  { value: "ITAU_CARD_XLSX", label: "Itaú — Cartão (XLSX)", accept: ".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" },
  { value: "SANTANDER_ACCOUNT_PDF", label: "Santander — Conta (PDF)", accept: ".pdf,application/pdf" },
  { value: "PAYROLL_PDF", label: "Contracheque (PDF)", accept: ".pdf,application/pdf" },
];

export default function ImportPanel({ accessToken }: ImportPanelProps) {
  const [adapter, setAdapter] = useState<SourceAdapter>("INTER_ACCOUNT_CSV");
  const [file, setFile] = useState<File | null>(null);
  const [targetId, setTargetId] = useState("");
  const [accounts, setAccounts] = useState<FinancialAccount[]>([]);
  const [cards, setCards] = useState<CreditCard[]>([]);
  const [result, setResult] = useState<ImportRegistration | null>(null);
  const [message, setMessage] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const accept = useMemo(() => adapterOptions.find((item) => item.value === adapter)?.accept, [adapter]);
  const targetType = adapter === "PAYROLL_PDF" ? "PAYROLL" : adapter.includes("CARD") ? "CARD" : "ACCOUNT";
  const targets = targetType === "ACCOUNT" ? accounts : targetType === "CARD" ? cards : [];

  useEffect(() => {
    let active = true;
    void Promise.all([
      apiRequest<FinancialAccount[]>(accessToken, "/api/accounts"),
      apiRequest<CreditCard[]>(accessToken, "/api/cards"),
    ]).then(([nextAccounts, nextCards]) => {
      if (!active) return;
      setAccounts(nextAccounts);
      setCards(nextCards);
    }).catch((error) => {
      if (active) setMessage(error instanceof Error ? error.message : "Não foi possível carregar contas e cartões.");
    });
    return () => { active = false; };
  }, [accessToken]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!file) return;
    setSubmitting(true);
    setMessage("Calculando a assinatura e registrando o arquivo…");
    setResult(null);
    try {
      const form = new FormData();
      form.set("adapter", adapter);
      if (targetType !== "PAYROLL") form.set("targetId", targetId);
      form.set("file", file);
      const registered = await apiRequest<ImportRegistration>(accessToken, "/api/imports/register", { method: "POST", body: form });
      setResult(registered);
      if (registered.duplicate) {
        setMessage("Este arquivo já havia sido registrado. Nenhuma cópia foi criada.");
      } else if (registered.executionStatus === "COMPLETED" || registered.executionStatus === "COMPLETED_WITH_WARNINGS") {
        setMessage(`Importação concluída: ${registered.importedRows} novo(s) e ${registered.duplicateRows} duplicado(s).`);
      } else {
        setMessage("Arquivo armazenado de forma privada e execução preparada.");
      }
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Não foi possível registrar o arquivo.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="workspace import-workspace" aria-labelledby="import-title">
      <header className="section-heading"><div><span className="section-kicker">IMPORTAÇÃO</span><h2 id="import-title">Registrar uma fonte</h2></div><span className="privacy-badge">Armazenamento privado</span></header>
      <p className="section-description">O extrato da conta Inter já é extraído e conciliado. Os demais formatos ficam armazenados com segurança enquanto seus adaptadores são habilitados.</p>
      <form className="import-form" onSubmit={submit}>
        <div><label htmlFor="adapter">Tipo de documento</label><select id="adapter" value={adapter} onChange={(event) => { setAdapter(event.target.value as SourceAdapter); setTargetId(""); setFile(null); setResult(null); }}>
          {adapterOptions.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
        </select></div>
        {targetType !== "PAYROLL" ? <div><label htmlFor="import-target">{targetType === "ACCOUNT" ? "Conta" : "Cartão"}</label><select id="import-target" value={targetId} onChange={(event) => setTargetId(event.target.value)} required><option value="">Selecione</option>{targets.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></div> : null}
        <div className="file-field"><label htmlFor="financial-file">Arquivo</label><input key={adapter} id="financial-file" type="file" accept={accept} onChange={(event) => setFile(event.target.files?.[0] ?? null)} required /><small>Até 10 MB. Acesso restrito ao usuário autenticado.</small></div>
        <button type="submit" className="button primary" disabled={!file || submitting || (targetType !== "PAYROLL" && !targetId)}>{submitting ? "Importando…" : "Importar arquivo"}</button>
      </form>
      {file ? <div className="selected-file"><span>{file.name}</span><small>{(file.size / 1024).toLocaleString("pt-BR", { maximumFractionDigits: 1 })} KB</small></div> : null}
      {result ? <dl className="import-result"><div><dt>Situação</dt><dd>{result.duplicate ? "Já registrado" : result.executionStatus}</dd></div><div><dt>Detectados</dt><dd>{result.detectedRows}</dd></div><div><dt>Novos</dt><dd>{result.importedRows}</dd></div><div><dt>Duplicados</dt><dd>{result.duplicateRows}</dd></div><div><dt>Recebido em</dt><dd>{new Date(result.receivedAt).toLocaleString("pt-BR")}</dd></div><div className="hash"><dt>SHA-256</dt><dd>{result.sha256}</dd></div></dl> : null}
      {message ? <p className="status" role="status">{message}</p> : null}
    </section>
  );
}
