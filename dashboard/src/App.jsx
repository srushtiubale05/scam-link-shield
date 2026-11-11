import { useState, useCallback } from "react";
import { getAuth, setAuth } from "./api";
import Home from "./Home";
import Stats from "./Stats";
import Login from "./Login";
import Reports from "./Reports";
import Admin from "./Admin";

export default function App() {
  const [auth, setAuthState] = useState(getAuth());
  const [page, setPage] = useState("home");
  const [toast, setToast] = useState(null);

  const notify = useCallback((text, ok = true) => {
    setToast({ text, ok });
    setTimeout(() => setToast(null), 3500);
  }, []);

  const afterAuth = () => { setAuthState(getAuth()); setPage("home"); };
  const logout = () => { setAuth(null); setAuthState(null); setPage("home"); notify("Logged out"); };

  const links = [["home", "Check a link"], ["stats", "Live stats"]];
  if (auth) links.push(["report", "Report a scam"]);
  if (auth?.role === "ADMIN") links.push(["admin", "Admin"]);

  return (
    <>
      <nav className="nav">
        <div className="nav-in">
          <div className="brand" onClick={() => setPage("home")}>🛡️ <span>ScamShield</span></div>
          <div className="links">
            {links.map(([id, label]) => (
              <button key={id} className={"link" + (page === id ? " active" : "")} onClick={() => setPage(id)}>{label}</button>
            ))}
            {auth ? (
              <>
                <span className="muted" style={{ margin: "0 8px" }}>👤 {auth.name}</span>
                <button className="btn ghost small" onClick={logout}>Log out</button>
              </>
            ) : (
              <button className="btn small" onClick={() => setPage("login")}>Log in</button>
            )}
          </div>
        </div>
      </nav>

      <div className="container">
        {page === "home" && <Home goReport={() => setPage(auth ? "report" : "login")} />}
        {page === "stats" && <Stats />}
        {page === "login" && !auth && <Login onDone={afterAuth} notify={notify} />}
        {page === "report" && auth && <Reports notify={notify} />}
        {page === "admin" && auth?.role === "ADMIN" && <Admin notify={notify} />}
      </div>

      {toast && <div className={"toast " + (toast.ok ? "good" : "err")}>{toast.text}</div>}
    </>
  );
}