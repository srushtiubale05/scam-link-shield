import { useEffect, useState } from "react";
import { request } from "./api";

const SAMPLES = ["fake-sbi-kyc.xyz/login", "hdfc-verify-account-now.xyz/login", "amazon.in", "fake-job-offer.in/apply"];

function useCount(target) {
  const [v, setV] = useState(0);
  useEffect(() => {
    let raf, start;
    const tick = (t) => {
      start ??= t;
      const p = Math.min(1, (t - start) / 900);
      setV(Math.round(target * (1 - Math.pow(1 - p, 3))));
      if (p < 1) raf = requestAnimationFrame(tick);
    };
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [target]);
  return v;
}

function Counter({ value, label }) {
  const v = useCount(value);
  return <div className="card stat"><b className="grad">{v}</b><span>{label}</span></div>;
}

function Result({ r }) {
  const cfg = {
    KNOWN_SCAM: ["scam", "🚨", "Known scam", "Confirmed by the community. Do not open this link."],
    HIGH_RISK: ["scam", "⚠️", "High risk", "Our AI model strongly flags this link as phishing."],
    SUSPICIOUS: ["warn", "🤔", "Looks suspicious", "Some warning signs. Be careful and verify the sender."],
    UNKNOWN: ["none", "🔍", "No warning found", "Not in our scam list. That does not guarantee it is safe."],
  }[r.verdict];
  const score = r.mlScore;
  const color = score >= 90 ? "var(--red)" : score >= 70 ? "var(--amber)" : "var(--green)";
  return (
    <div className={"result " + cfg[0]}>
      <h3>{cfg[1]} {cfg[2]}</h3>
      <div className="muted">{cfg[3]}</div>
      <div className="mono" style={{ marginTop: 8, fontSize: 13 }}>{r.normalizedUrl}</div>
      {r.verdict === "KNOWN_SCAM" && (
        <p style={{ margin: "8px 0 0" }}>Category: <b>{r.category.replace("_", " ")}</b> · confirmed by <b>{r.confirmations}</b> user(s)</p>
      )}
      {score != null && (
        <>
          <div className="meter"><i style={{ width: score + "%", background: color }} /></div>
          <div className="muted" style={{ fontSize: 13 }}>AI risk score: {score}/100</div>
        </>
      )}
    </div>
  );
}

export default function Home({ goReport }) {
  const [url, setUrl] = useState("");
  const [busy, setBusy] = useState(false);
  const [res, setRes] = useState(null);
  const [err, setErr] = useState("");
  const [stats, setStats] = useState(null);

  useEffect(() => { request("/api/stats").then(setStats).catch(() => {}); }, []);

  async function check(value = url) {
    if (!value.trim()) return;
    setBusy(true); setErr(""); setRes(null);
    try {
      const [r] = await Promise.all([
        request("/api/check?url=" + encodeURIComponent(value.trim())),
        new Promise((ok) => setTimeout(ok, 700)),            // let the scan animation play
      ]);
      setRes(r);
    } catch (e) { setErr(e.message); }
    setBusy(false);
  }

  return (
    <div className="fade">
      <section className="hero">
        <div className="shield">🛡️</div>
        <h1>Is that link <span className="grad">a scam?</span></h1>
        <p>Paste any link from WhatsApp, SMS or email. We check it against community-verified scams and an AI model, before you click.</p>

        <div className="checker">
          <div className="row">
            <input className="input" placeholder="Paste a suspicious link..." value={url}
                   onChange={(e) => setUrl(e.target.value)} onKeyDown={(e) => e.key === "Enter" && check()} />
            <button className="btn" disabled={busy} onClick={() => check()}>{busy ? "Scanning..." : "Check link"}</button>
          </div>
          <div className="chips">
            <span className="muted" style={{ fontSize: 12 }}>Try:</span>
            {SAMPLES.map((s) => (
              <button key={s} className="chip" onClick={() => { setUrl(s); check(s); }}>{s}</button>
            ))}
          </div>
          {busy && <div className="scan"><i /></div>}
          {err && <div className="result scam">{err}</div>}
          {res && <Result r={res} />}
        </div>
      </section>

      {stats && (
        <div className="grid g3" style={{ marginTop: 40 }}>
          <Counter value={stats.verified} label="Verified scams blocked" />
          <Counter value={stats.pending} label="Reports under review" />
          <Counter value={stats.recent.reduce((a, r) => a + r.confirmationCount, 0)} label="Community confirmations" />
        </div>
      )}

      <h2 className="sec">How it works</h2>
      <div className="grid g3">
        <div className="card step fade"><div className="n">1</div><h3>Community reports</h3>
          <p className="muted">Anyone can report a scam link. When enough people confirm it, it becomes verified.</p></div>
        <div className="card step fade"><div className="n">2</div><h3>Instant local check</h3>
          <p className="muted">Our browser extension holds a tiny Bloom filter and checks every page in microseconds, without sending your browsing history anywhere.</p></div>
        <div className="card step fade"><div className="n">3</div><h3>AI for new scams</h3>
          <p className="muted">Brand-new links are scored by a machine-learning model, so fresh scams get flagged too.</p></div>
      </div>

      <div style={{ textAlign: "center", marginTop: 40 }}>
        <button className="btn" onClick={goReport}>Help the community: report a scam →</button>
      </div>
    </div>
  );
}