import { useMemo, useState, type FormEvent } from "react";
import { apiRequest } from "../lib/api";
import type { ImportRegistration, SourceAdapter } from "../types";

interface ImportPanelProps { accessToken: string; }

const adapterOptions: Array<{ value: SourceAdapter; label: string; accept: string }> = [
  { value: "INTER_ACCOUNT_CSV", label: "Conta — CSV", accept: ".csv,text/csv" },
  { value: "INTER_CARD_CSV", label: "Cartão — CSV", accept: ".csv,text/csv" },
  { value: "ITAU_CARD_XLSX", label: "Cartão — XLSX", accept: ".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" },
  { value: "SANTANDER_ACCOUNT_PDF", label: "Conta — PDF", accept: ".pdf,application/pdf" },
  { value: "PAYROLL_PDF", label: "Remuneração — PDF", accept: ".pdf,application/pdf" },
];

export default function ImportPanel({ accessToken }: ImportPanelProps) {
  const [adapter, setAdapter] = useState<SourceAdapter>("INTER_ACCOUNT_CSV");
  const [file, setFile] = useState<File | null>(null);
  const [result, setResult] = useState<ImportRegistration | null>(null);
  const [message, setMessage] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const accept = useMemo(() => adapterOptions.find((item) => item.value === adapter)?.accept, [adapter]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!file) return;
    setSubmitting(true);
    setMessage("Calculando a assinatura e registrando o arquivo…");
    setResult(null);
    try {
      const form = new FormData();
      form.set("adapter", adapter);
      form.set("file", file);
      const registered = await apiRequest<ImportRegistration>(accessToken, "/api/imports/register", { method: "POST", body: form });
      setResult(registered);
      setMessage(registered.duplicate ? "Este arquivo já havia sido registrado. Nenhuma cópia foi criada." : "Arquivo registrado com segurança.");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Não foi possível registrar o arquivo.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="workspace import-workspace" aria-labelledby="import-title">
      <header className="section-heading"><div><span className="section-kicker">IMPORTAÇÃO</span><h2 id="import-title">Registrar uma fonte</h2></div><span className="privacy-badge">Arquivo descartado</span></header>
      <p className="section-description">O backend calcula o SHA-256 e guarda apenas os metadados. A extração e revisão dos lançamentos serão habilitadas no próximo marco.</p>
      <form className="import-form" onSubmit={submit}>
        <div><label htmlFor="adapter">Tipo de documento</label><select id="adapter" value={adapter} onChange={(event) => { setAdapter(event.target.value as SourceAdapter); setFile(null); setResult(null); }}>
          {adapterOptions.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
        </select></div>
        <div className="file-field"><label htmlFor="financial-file">Arquivo</label><input key={adapter} id="financial-file" type="file" accept={accept} onChange={(event) => setFile(event.target.files?.[0] ?? null)} required /><small>Até 10 MB. O conteúdo original não permanece armazenado.</small></div>
        <button type="submit" className="button primary" disabled={!file || submitting}>{submitting ? "Registrando…" : "Registrar arquivo"}</button>
      </form>
      {file ? <div className="selected-file"><span>{file.name}</span><small>{(file.size / 1024).toLocaleString("pt-BR", { maximumFractionDigits: 1 })} KB</small></div> : null}
      {result ? <dl className="import-result"><div><dt>Situação</dt><dd>{result.duplicate ? "Já registrado" : "Novo"}</dd></div><div><dt>Recebido em</dt><dd>{new Date(result.receivedAt).toLocaleString("pt-BR")}</dd></div><div className="hash"><dt>SHA-256</dt><dd>{result.sha256}</dd></div></dl> : null}
      {message ? <p className="status" role="status">{message}</p> : null}
    </section>
  );
}
