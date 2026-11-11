import { useState } from "react";
import { request, setAuth } from "./api";

export default function Login({ onDone }) {
  const [mode, setMode] = useState("login");
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  async function submit(e) {
    e.preventDefault();
    setError("");
    try {
      const body = mode === "login" ? { email, password } : { name, email, password };
      const data = await request(`/api/auth/${mode}`, { method: "POST", body });
      setAuth({ token: data.token, name: data.name, email: data.email, role: data.role });
      onDone();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <form onSubmit={submit} style={{ maxWidth: 320, display: "grid" }}>
      <h2>{mode === "login" ? "Log in" : "Create account"}</h2>
      {mode === "register" && (
        <input placeholder="Name" value={name} onChange={(e) => setName(e.target.value)} required />
      )}
      <input type="email" placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} required />
      <input type="password" placeholder="Password (min 8 characters)" value={password}
             onChange={(e) => setPassword(e.target.value)} required />
      <button type="submit">{mode === "login" ? "Log in" : "Register"}</button>
      <button type="button" className="secondary"
              onClick={() => { setMode(mode === "login" ? "register" : "login"); setError(""); }}>
        {mode === "login" ? "Need an account? Register" : "Have an account? Log in"}
      </button>
      {error && <p className="err">{error}</p>}
    </form>
  );
}