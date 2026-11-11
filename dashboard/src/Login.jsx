import { useState } from "react";
import { request, setAuth } from "./api";

export default function Login({ onDone, notify }) {
  const [mode, setMode] = useState("login");
  const [f, setF] = useState({ name: "", email: "", password: "" });
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    try {
      const body = mode === "login" ? { email: f.email, password: f.password } : f;
      const d = await request(`/api/auth/${mode}`, { method: "POST", body });
      setAuth({ token: d.token, name: d.name, email: d.email, role: d.role });
      notify(`Welcome, ${d.name}! 🎉`);
      onDone();
    } catch (err) { notify(err.message, false); }
    setBusy(false);
  }

  return (
    <div className="auth fade">
      <div className="auth-side">
        <div style={{ fontSize: 54 }}>🛡️</div>
        <h2>Join the fight against online scams</h2>
        <ul style={{ padding: 0 }}>
          <li>✅ Report scam links in seconds</li>
          <li>✅ Help verify reports from others</li>
          <li>✅ Protect friends and family</li>
        </ul>
      </div>
      <form className="auth-form" onSubmit={submit}>
        <h2>{mode === "login" ? "Welcome back" : "Create your account"}</h2>
        {mode === "register" && <input className="input" placeholder="Your name" value={f.name} onChange={set("name")} required />}
        <input className="input" type="email" placeholder="Email" value={f.email} onChange={set("email")} required />
        <input className="input" type="password" placeholder="Password (min 8 characters)" value={f.password} onChange={set("password")} required />
        <button className="btn" disabled={busy}>{busy ? "Please wait..." : mode === "login" ? "Log in" : "Sign up"}</button>
        <button type="button" className="btn ghost" onClick={() => setMode(mode === "login" ? "register" : "login")}>
          {mode === "login" ? "New here? Create an account" : "Already have an account? Log in"}
        </button>
      </form>
    </div>
  );
}