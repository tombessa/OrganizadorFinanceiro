import { useCallback, useEffect, useState } from "react";
import type { Session } from "@supabase/supabase-js";
import AuthPanel from "./components/AuthPanel";
import FinancialSetup from "./components/FinancialSetup";
import ImportPanel from "./components/ImportPanel";
import { apiBaseUrl, apiHealth } from "./lib/api";
import { requireSupabase } from "./lib/supabase";
import type { SetupCounts } from "./types";

const emptyCounts: SetupCounts = { institutions: 0, accounts: 0, cards: 0 };

export default function App() {
  const [session, setSession] = useState<Session | null>(null);
  const [authReady, setAuthReady] = useState(false);
  const [authMessage, setAuthMessage] = useState("");
  const [apiOnline, setApiOnline] = useState<boolean | null>(null);
  const [counts, setCounts] = useState<SetupCounts>(emptyCounts);
  const updateCounts = useCallback((nextCounts: SetupCounts) => setCounts(nextCounts), []);

  useEffect(() => {
    let active = true;
    try {
      const client = requireSupabase();
      void client.auth.getSession().then(({ data, error }) => {
        if (!active) return;
        if (error) setAuthMessage(error.message);
        setSession(data.session);
        setAuthReady(true);
      });
      const { data: listener } = client.auth.onAuthStateChange((_event, nextSession) => {
        if (active) setSession(nextSession);
      });
      return () => { active = false; listener.subscription.unsubscribe(); };
    } catch (error) {
      setAuthMessage(error instanceof Error ? error.message : "Supabase não configurado.");
      setAuthReady(true);
    }
  }, []);

  useEffect(() => { void apiHealth().then(setApiOnline); }, []);

  return (
    <main>
      <section className="topbar">
        <div className="brand"><span className="brand-mark">OF</span><div><span className="eyebrow">ORGANIZADOR FINANCEIRO</span><small>Clareza para decidir melhor</small></div></div>
        <AuthPanel session={session} onSessionChange={setSession} />
      </section>
      <section className="hero">
        <div><span className="hero-label">CONTROLE PESSOAL E SEGURO</span><h1>Entenda seu dinheiro antes de tentar consertá-lo.</h1><p>Organize contas, cartões e documentos financeiros em um só lugar, com acesso protegido e histórico auditável.</p></div>
        <div className="connection-card"><span className={`connection-dot ${apiOnline === false ? "offline" : ""}`} /><div><strong>{apiOnline === null ? "Verificando API…" : apiOnline ? "Backend conectado" : "Backend indisponível"}</strong><small>{apiBaseUrl.replace(/^https?:\/\//, "")}</small></div></div>
      </section>
      {!authReady ? <section className="welcome-card"><p>Verificando sua sessão…</p></section> : session ? (
        <>
          <section className="metric-grid" aria-label="Resumo dos cadastros">
            <article><span>Instituições</span><strong>{counts.institutions}</strong><small>bancos e instituições</small></article>
            <article><span>Contas</span><strong>{counts.accounts}</strong><small>fontes de saldo</small></article>
            <article><span>Cartões</span><strong>{counts.cards}</strong><small>faturas organizadas</small></article>
          </section>
          <FinancialSetup accessToken={session.access_token} onCountsChange={updateCounts} />
          <ImportPanel accessToken={session.access_token} />
        </>
      ) : (
        <section className="welcome-card">
          <div><span className="section-kicker">PRIMEIRO PASSO</span><h2>Entre para acessar seu espaço financeiro.</h2><p>Seus cadastros ficam separados por usuário. O frontend utiliza o Supabase somente para autenticação; os dados financeiros passam pela API protegida.</p></div>
          {authMessage ? <p className="status error" role="alert">{authMessage}</p> : <ol><li>Crie ou acesse sua conta.</li><li>Cadastre suas instituições e contas.</li><li>Registre seus documentos com segurança.</li></ol>}
        </section>
      )}
      <footer>Organizador Financeiro · seus arquivos originais não são retidos</footer>
    </main>
  );
}
