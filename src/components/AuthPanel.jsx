import { useState } from "react";
import { requireSupabase } from "../lib/supabase.js";

export default function AuthPanel({ session, onSessionChange }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [mode, setMode] = useState("login");
  const [message, setMessage] = useState("");

  async function submit(event) {
    event.preventDefault();
    setMessage("");
    try {
      const client = requireSupabase();
      const result = mode === "login"
        ? await client.auth.signInWithPassword({ email, password })
        : await client.auth.signUp({ email, password });
      if (result.error) throw result.error;
      if (result.data.session) onSessionChange(result.data.session);
      setMessage(mode === "login" ? "Sessão iniciada." : "Cadastro criado. Confirme seu e-mail, se solicitado.");
    } catch (error) {
      setMessage(error.message);
    }
  }

  if (session) return <div className="session"><span>{session.user.email}</span><button onClick={async () => { await requireSupabase().auth.signOut(); onSessionChange(null); }}>Sair</button></div>;

  return <form className="auth" onSubmit={submit}>
    <input aria-label="E-mail" type="email" placeholder="seu e-mail" value={email} onChange={(e) => setEmail(e.target.value)} required />
    <input aria-label="Senha" type="password" placeholder="senha (mínimo de 6)" value={password} onChange={(e) => setPassword(e.target.value)} minLength="6" required />
    <button type="submit">{mode === "login" ? "Entrar" : "Criar acesso"}</button>
    <button type="button" className="link" onClick={() => setMode(mode === "login" ? "signup" : "login")}>{mode === "login" ? "Primeiro acesso" : "Já tenho acesso"}</button>
    {message && <small>{message}</small>}
  </form>
}
