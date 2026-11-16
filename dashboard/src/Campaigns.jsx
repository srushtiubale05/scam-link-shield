import { useEffect, useState } from "react";
import { request } from "./api";

const LABEL = { domain: "same domain", host: "same host", kit: "same page path" };

function why(key) {
  const i = key.indexOf(":");
  return `${LABEL[key.slice(0, i)] || key.slice(0, i)}: ${key.slice(i + 1)}`;
}

export default function Campaigns() {
  const [list, setList] = useState(null);
  const [err, setErr] = useState("");

  useEffect(() => {
    request("/api/campaigns?limit=20").then(setList).catch((e) => setErr(e.message));
  }, []);

  if (err) return <p style={{ color: "var(--red)" }}>{err}</p>;
  if (!list) return <p className="muted">Loading...</p>;

  return (
    <div className="fade">
      <h2 className="sec">Scam campaigns</h2>
      <p className="muted">
        Scams that share a domain or the same phishing-kit page path are grouped into one campaign.
        The reason for each link is shown, so the grouping is explainable.
      </p>

      {list.length === 0 && <div className="card" style={{ marginTop: 16 }}>No linked campaigns found yet.</div>}

      <div className="list" style={{ marginTop: 16 }}>
        {list.map((c) => (
          <div className="card" key={c.id}>
            <div style={{ display: "flex", gap: 10, alignItems: "center", flexWrap: "wrap" }}>
              <b style={{ fontSize: 22 }}>{c.size} linked scams</b>
              {c.brand && <span className="badge cat">targets: {c.brand}</span>}
              {c.category && <span className="badge cat">{c.category.replace("_", " ")}</span>}
              <span className="muted">{c.domains} domain(s) · via {c.sources.join(", ")}</span>
            </div>

            <div className="chips">
              <span className="muted" style={{ fontSize: 12 }}>Linked by:</span>
              {c.linkedBy.map((k) => <span className="chip" key={k}>{why(k)}</span>)}
            </div>

            {/* plain text on purpose: never make scam links clickable */}
            <div className="mono muted" style={{ marginTop: 10, fontSize: 13 }}>
              {c.samples.map((s) => <div key={s}>{s}</div>)}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}