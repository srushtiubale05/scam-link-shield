import { useState } from "react";
import { getAuth, setAuth } from "./api";
import Stats from "./Stats";
import Login from "./Login";
import Reports from "./Reports";
import Admin from "./Admin";

export default function App() {
  const [auth, setAuthState] = useState(getAuth());
  const [tab, setTab] = useState("overview");

  const refresh = () => { setAuthState(getAuth()); setTab("overview"); };
  const logout = () => { setAuth(null); refresh(); };

  const tabs = [["overview", "Overview"]];
  if (auth) tabs.push(["reports", "Report & my reports"]);
  if (auth?.role === "ADMIN") tabs.push(["admin", "Admin"]);
  if (!auth) tabs.push(["login", "Log in"]);

  return (
    <div style={{ maxWidth: 900, margin: "0 auto", padding: 24 }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <h1>🛡️ Scam Link Shield</h1>
        {auth && (
          <div>
            {auth.name} ({auth.role}) <button className="secondary" onClick={logout}>Log out</button>
          </div>
        )}
      </div>

      <div style={{ display: "flex", gap: 8, marginBottom: 16 }}>
        {tabs.map(([id, label]) => (
          <button key={id} className={tab === id ? "" : "secondary"} onClick={() => setTab(id)}>
            {label}
          </button>
        ))}
      </div>

      {tab === "overview" && <Stats />}
      {tab === "login" && !auth && <Login onDone={refresh} />}
      {tab === "reports" && auth && <Reports />}
      {tab === "admin" && auth?.role === "ADMIN" && <Admin />}
    </div>
  );
}