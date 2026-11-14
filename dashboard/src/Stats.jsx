import { useEffect, useState } from "react";
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from "recharts";
import { request } from "./api";

const COLORS = ["#7c5cff", "#22d3ee", "#2ee59d", "#ffb020", "#ff4d6d"];

export default function Stats() {
  const [s, setS] = useState(null);
  const [err, setErr] = useState("");
  useEffect(() => { request("/api/stats").then(setS).catch((e) => setErr(e.message)); }, []);

  if (err) return <p style={{ color: "var(--red)" }}>{err}</p>;
  if (!s) return <p className="muted">Loading...</p>;

  const chart = Object.entries(s.byCategory).map(([name, count]) => ({ name: name.replace("_", " "), count }));

  return (
    <div className="fade">
      <h2 className="sec">Live scam statistics</h2>
      <div className="grid g3">
        <div className="card stat"><b style={{ color: "var(--red)" }}>{s.verified}</b><span>Verified scams</span></div>
        <div className="card stat"><b style={{ color: "var(--amber)" }}>{s.pending}</b><span>Pending review</span></div>
        <div className="card stat"><b className="muted">{s.rejected}</b><span>Rejected</span></div>
      </div>

      <h2 className="sec">Scams by category</h2>
      <div className="card" style={{ height: 300 }}>
        <ResponsiveContainer>
          <BarChart data={chart}>
            <XAxis dataKey="name" stroke="#8e98c4" />
            <YAxis allowDecimals={false} stroke="#8e98c4" />
            <Tooltip contentStyle={{ background: "#182042", border: "1px solid #26305a", borderRadius: 10 }} />
            <Bar dataKey="count" radius={[8, 8, 0, 0]}>
              {chart.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>

      <h2 className="sec">Recently verified</h2>
      <div className="list">
        {s.recent.map((r) => (
          <div className="item" key={r.id}>
            <span>🚫</span>
            <div className="grow mono">{r.normalizedUrl}</div>
            <span className="badge cat">{r.category.replace("_", " ")}</span>
            <span className="muted">
              {r.source && r.source !== "COMMUNITY" ? `via ${r.source}` : `${r.confirmationCount} confirmations`}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}