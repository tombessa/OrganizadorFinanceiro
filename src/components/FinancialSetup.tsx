import { useCallback, useEffect, useMemo, useState, type FormEvent } from "react";
import { apiRequest } from "../lib/api";
import type { AccountType, CreditCard, FinancialAccount, Institution, SetupCounts } from "../types";

interface FinancialSetupProps {
  accessToken: string;
  onCountsChange: (counts: SetupCounts) => void;
}

const money = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });

export default function FinancialSetup({ accessToken, onCountsChange }: FinancialSetupProps) {
  const [institutions, setInstitutions] = useState<Institution[]>([]);
  const [accounts, setAccounts] = useState<FinancialAccount[]>([]);
  const [cards, setCards] = useState<CreditCard[]>([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");
  const [institutionCode, setInstitutionCode] = useState("");
  const [institutionName, setInstitutionName] = useState("");
  const [accountInstitutionId, setAccountInstitutionId] = useState("");
  const [accountName, setAccountName] = useState("");
  const [accountType, setAccountType] = useState<AccountType>("CHECKING");
  const [overdraftLimit, setOverdraftLimit] = useState("0");
  const [cardInstitutionId, setCardInstitutionId] = useState("");
  const [paymentAccountId, setPaymentAccountId] = useState("");
  const [cardName, setCardName] = useState("");
  const [lastFourDigits, setLastFourDigits] = useState("");
  const [closingDay, setClosingDay] = useState("1");
  const [dueDay, setDueDay] = useState("1");

  const institutionNames = useMemo(() => new Map(institutions.map((item) => [item.id, item.name])), [institutions]);

  const load = useCallback(async () => {
    setLoading(true);
    setMessage("");
    try {
      const [nextInstitutions, nextAccounts, nextCards] = await Promise.all([
        apiRequest<Institution[]>(accessToken, "/api/institutions"),
        apiRequest<FinancialAccount[]>(accessToken, "/api/accounts"),
        apiRequest<CreditCard[]>(accessToken, "/api/cards"),
      ]);
      setInstitutions(nextInstitutions);
      setAccounts(nextAccounts);
      setCards(nextCards);
      onCountsChange({ institutions: nextInstitutions.length, accounts: nextAccounts.length, cards: nextCards.length });
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Não foi possível carregar seus cadastros.");
    } finally {
      setLoading(false);
    }
  }, [accessToken, onCountsChange]);

  useEffect(() => {
    void load();
  }, [load]);

  async function createInstitution(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage("Salvando instituição…");
    try {
      await apiRequest(accessToken, "/api/institutions", {
        method: "POST",
        body: JSON.stringify({ code: institutionCode.trim().toUpperCase(), name: institutionName.trim() }),
      });
      setInstitutionCode("");
      setInstitutionName("");
      setMessage("Instituição cadastrada.");
      await load();
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Não foi possível cadastrar a instituição.");
    }
  }

  async function createAccount(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage("Salvando conta…");
    try {
      await apiRequest(accessToken, "/api/accounts", {
        method: "POST",
        body: JSON.stringify({
          institutionId: accountInstitutionId,
          name: accountName.trim(),
          accountType,
          overdraftLimit: Number(overdraftLimit.replace(",", ".")),
        }),
      });
      setAccountName("");
      setOverdraftLimit("0");
      setMessage("Conta cadastrada.");
      await load();
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Não foi possível cadastrar a conta.");
    }
  }

  async function createCard(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage("Salvando cartão…");
    try {
      await apiRequest(accessToken, "/api/cards", {
        method: "POST",
        body: JSON.stringify({
          institutionId: cardInstitutionId,
          paymentAccountId: paymentAccountId || null,
          name: cardName.trim(),
          lastFourDigits,
          closingDay: Number(closingDay),
          dueDay: Number(dueDay),
        }),
      });
      setCardName("");
      setLastFourDigits("");
      setMessage("Cartão cadastrado.");
      await load();
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Não foi possível cadastrar o cartão.");
    }
  }

  return (
    <section className="workspace" aria-labelledby="setup-title">
      <header className="section-heading">
        <div><span className="section-kicker">FUNDAÇÃO</span><h2 id="setup-title">Sua estrutura financeira</h2></div>
        <button type="button" className="button secondary" onClick={() => void load()} disabled={loading}>
          {loading ? "Atualizando…" : "Atualizar"}
        </button>
      </header>

      <div className="setup-grid">
        <form className="setup-card" onSubmit={createInstitution}>
          <div className="step-number">01</div><h3>Instituição</h3><p>Cadastre o banco ou instituição.</p>
          <label htmlFor="institution-code">Código</label>
          <input id="institution-code" value={institutionCode} onChange={(event) => setInstitutionCode(event.target.value)} placeholder="Ex.: BANCO" maxLength={40} required />
          <label htmlFor="institution-name">Nome</label>
          <input id="institution-name" value={institutionName} onChange={(event) => setInstitutionName(event.target.value)} placeholder="Nome da instituição" maxLength={120} required />
          <button className="button primary" type="submit">Cadastrar instituição</button>
        </form>

        <form className="setup-card" onSubmit={createAccount}>
          <div className="step-number">02</div><h3>Conta</h3><p>Saldo próprio e limite permanecem separados.</p>
          <label htmlFor="account-institution">Instituição</label>
          <select id="account-institution" value={accountInstitutionId} onChange={(event) => setAccountInstitutionId(event.target.value)} required disabled={!institutions.length}>
            <option value="">Selecione</option>{institutions.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
          </select>
          <label htmlFor="account-name">Nome da conta</label>
          <input id="account-name" value={accountName} onChange={(event) => setAccountName(event.target.value)} placeholder="Conta principal" maxLength={120} required />
          <div className="field-row"><div><label htmlFor="account-type">Tipo</label><select id="account-type" value={accountType} onChange={(event) => setAccountType(event.target.value as AccountType)}><option value="CHECKING">Conta corrente</option><option value="CASH">Dinheiro</option><option value="INVESTMENT">Investimento</option></select></div>
            <div><label htmlFor="overdraft-limit">Limite</label><input id="overdraft-limit" type="number" min="0" step="0.01" value={overdraftLimit} onChange={(event) => setOverdraftLimit(event.target.value)} /></div></div>
          <button className="button primary" type="submit" disabled={!institutions.length}>Cadastrar conta</button>
        </form>

        <form className="setup-card" onSubmit={createCard}>
          <div className="step-number">03</div><h3>Cartão</h3><p>Informe apenas os quatro últimos dígitos.</p>
          <label htmlFor="card-institution">Instituição</label>
          <select id="card-institution" value={cardInstitutionId} onChange={(event) => setCardInstitutionId(event.target.value)} required disabled={!institutions.length}>
            <option value="">Selecione</option>{institutions.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
          </select>
          <label htmlFor="card-name">Nome do cartão</label>
          <input id="card-name" value={cardName} onChange={(event) => setCardName(event.target.value)} placeholder="Cartão principal" maxLength={120} required />
          <label htmlFor="last-four">Últimos quatro dígitos</label>
          <input id="last-four" inputMode="numeric" pattern="[0-9]{4}" maxLength={4} value={lastFourDigits} onChange={(event) => setLastFourDigits(event.target.value.replace(/\D/g, ""))} placeholder="0000" required />
          <label htmlFor="payment-account">Conta de pagamento (opcional)</label>
          <select id="payment-account" value={paymentAccountId} onChange={(event) => setPaymentAccountId(event.target.value)}><option value="">Nenhuma</option>{accounts.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select>
          <div className="field-row"><div><label htmlFor="closing-day">Fechamento</label><input id="closing-day" type="number" min="1" max="31" value={closingDay} onChange={(event) => setClosingDay(event.target.value)} required /></div><div><label htmlFor="due-day">Vencimento</label><input id="due-day" type="number" min="1" max="31" value={dueDay} onChange={(event) => setDueDay(event.target.value)} required /></div></div>
          <button className="button primary" type="submit" disabled={!institutions.length}>Cadastrar cartão</button>
        </form>
      </div>

      <div className="registry-grid">
        <article><h3>Instituições</h3>{institutions.length ? <ul>{institutions.map((item) => <li key={item.id}><span>{item.name}</span><small>{item.code}</small></li>)}</ul> : <p className="empty">Nenhuma instituição cadastrada.</p>}</article>
        <article><h3>Contas</h3>{accounts.length ? <ul>{accounts.map((item) => <li key={item.id}><span>{item.name}</span><small>{institutionNames.get(item.institutionId)} · {money.format(item.overdraftLimit)} de limite</small></li>)}</ul> : <p className="empty">Nenhuma conta cadastrada.</p>}</article>
        <article><h3>Cartões</h3>{cards.length ? <ul>{cards.map((item) => <li key={item.id}><span>{item.name} · •••• {item.lastFourDigits}</span><small>{institutionNames.get(item.institutionId)} · vence dia {item.dueDay}</small></li>)}</ul> : <p className="empty">Nenhum cartão cadastrado.</p>}</article>
      </div>
      {message ? <p className="status" role="status">{message}</p> : null}
    </section>
  );
}
