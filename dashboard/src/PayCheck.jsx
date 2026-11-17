import { useState } from "react";
import { request } from "./api";

export default function PayCheck() {
  const [value, setValue] = useState("");
  const [res, setRes] = useState(null);
  const [err, setErr] = useState("");
  const [busy, setBusy] = useState(false);

  async function check(e) {
    e.preventDefault();
    setBusy(true); setErr(""); setRes(null);
    try {
      setRes(await request("/api/indicators/check?value=" + encodeURIComponent(value.trim())));
    } catch (e2) { setErr(e2.message); }
    setBusy(false);
  }

  return (
    <div className="fade">
      <h2 className="sec">Check before you pay</h2>
      <p className="muted">Enter a UPI ID (name@bank) or a mobile number to see if it was reported in scams.</p>

      <form className="card" onSubmit={check} style={{ marginTop: 16 }}>
        <div className="row">
          <input className="input" placeholder="e.g. refund.help@okaxis or 98765 43210" value={value}
                 onChange={(e) => setValue(e.target.value)} required />
          <button className="btn" disabled={busy}>{busy ? "Checking..." : "Check"}</button>
        </div>
        {err && <div className="result scam">{err}</div>}
        {res && (
          <div className={"result " + (res.verdict === "REPORTED" ? "scam" : "none")}>
            <h3>{res.verdict === "REPORTED" ? "🚨 Reported in scams" : "🔍 No reports found"}</h3>
            <div className="mono" style={{ fontSize: 13 }}>{res.type}: {res.value}</div>
            {res.verdict === "REPORTED"
              ? <p style={{ margin: "8px 0 0" }}>
                  Linked to <b>{res.verifiedReports}</b> verified scam report(s): {res.categories.map((c) => c.replace("_", " ")).join(", ")}.
                  Do not send money.
                </p>
              : <p className="muted" style={{ margin: "8px 0 0" }}>No reports does not mean it is safe. Verify the person before paying.</p>}
          </div>
        )}
      </form>
    </div>
  );
}