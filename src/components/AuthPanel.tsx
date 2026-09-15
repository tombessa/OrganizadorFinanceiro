import { useState, type FormEvent } from "react";
import type { Session } from "@supabase/supabase-js";
import { requireSupabase } from "../lib/supabase";

interface AuthPanelProps {
  session: Session | null;
  onSessionChange: (session: Session | null) => void;
}

export default function AuthPanel({ session, onSessionChange }: AuthPanelProps) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [mode, setMode] = useState<"login" | "signup">("login");
  const [message, setMessage] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage("");
    setSubmitting(true);
    try {
      const client = requireSupabase();
      const result = mode === "login"
        ? await client.auth.signInWithPassword({ email, password })
        : await client.auth.signUp({ email, password });
      if (result.error) throw result.error;
      if (result.data.session) onSessionChange(result.data.session);
      setMessage(mode === "login" ? "Sessão iniciada." : "Cadastro criado. Confirme seu e-mail, se solicitado.");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Não foi possível autenticar.");
    } finally {
      setSubmitting(false);
    }
  }

  async function signOut() {
    await requireSupabase().auth.signOut();
    onSessionChange(null);
  }

  if (session) {
    return (
      <div className="session">
        <span title={session.user.email}>{session.user.email}</span>
        <button type="button" className="button secondary" onClick={signOut}>Sair</button>
      </div>
    );
  }

  return (
    <form className="auth" onSubmit={submit}>
      <label className="sr-only" htmlFor="auth-email">E-mail</label>
      <input id="auth-email" type="email" placeholder="seu e-mail" autoComplete="email" value={email}
        onChange={(event) => setEmail(event.target.value)} required />
      <label className="sr-only" htmlFor="auth-password">Senha</label>
      <input id="auth-password" type="password" placeholder="senha (mínimo de 6)" autoComplete={mode === "login" ? "current-password" : "new-password"}
        value={password} onChange={(event) => setPassword(event.target.value)} minLength={6} required />
      <button type="submit" className="button primary" disabled={submitting}>
        {submitting ? "Aguarde…" : mode === "login" ? "Entrar" : "Criar acesso"}
      </button>
      <button type="button" className="button link" onClick={() => setMode((current) => current === "login" ? "signup" : "login")}>
        {mode === "login" ? "Primeiro acesso" : "Já tenho acesso"}
      </button>
      {message ? <small className="auth-message" role="status">{message}</small> : null}
    </form>
  );
}
