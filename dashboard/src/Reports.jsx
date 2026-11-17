import { useEffect, useState } from "react";
import { request, CATEGORIES } from "./api";

const ICONS = { PHISHING: "🎣", FAKE_JOB: "💼", UPI_FRAUD: "💸", LOTTERY_SCAM: "🎰", OTHER: "❓" };

export default function Reports({ notify }) {
  const [url, setUrl] = useState("");
  const [upi, setUpi] = useState("");
  const [phone, setPhone] = useState("");
  const [category, setCategory] = useState(CATEGORIES[0]);
  const [mine, setMine] = useState([]);

  const load = () => request("/api/reports/mine").then(setMine).catch(() => {});
  useEffect(() => { load(); }, []);

  async function submit(e) {
    e.preventDefault();
    try {
      const body = { url, category };
      if (upi.trim()) body.upiId = upi.trim();
      if (phone.trim()) body.phone = phone.trim();
      const r = await request("/api/reports", { method: "POST", body });
      notify(`Thanks! 🙌 Confirmations: ${r.confirmationCount} · ${r.status}`);
      setUrl(""); setUpi(""); setPhone(""); load();
    } catch (err) { notify(err.message, false); }
  }

  return (
    <div className="fade">
      <h2 className="sec">Report a scam link</h2>
      <form className="card grid" onSubmit={submit}>
        <input className="input" placeholder="https://suspicious-link.xyz/login" value={url}
               onChange={(e) => setUrl(e.target.value)} required />
        <div className="chips" style={{ marginTop: 0 }}>
          {CATEGORIES.map((c) => (
            <button type="button" key={c} className={"chip" + (category === c ? " on" : "")} onClick={() => setCategory(c)}>
              {ICONS[c]} {c.replace("_", " ")}
            </button>
          ))}
        </div>
        <div className="muted" style={{ fontSize: 13 }}>
          Optional: did the page ask you to pay? Adding the payment details helps link related scams.
        </div>
        <div className="row">
          <input className="input" placeholder="UPI ID seen (name@bank)" value={upi} onChange={(e) => setUpi(e.target.value)} />
          <input className="input" placeholder="Phone number seen" value={phone} onChange={(e) => setPhone(e.target.value)} />
        </div>
        <div><button className="btn">Submit report</button></div>
      </form>

      <h2 className="sec">My reports</h2>
      {mine.length === 0 && <p className="muted">You have not reported anything yet.</p>}
      <div className="list">
        {mine.map((r) => (
          <div className="item" key={r.id}>
            <span>{ICONS[r.category]}</span>
            <div className="grow mono">{r.normalizedUrl}</div>
            <span className="muted">{r.confirmationCount} confirmations</span>
            <span className={"badge " + r.status}>{r.status}</span>
          </div>
        ))}
      </div>
    </div>
  );
}