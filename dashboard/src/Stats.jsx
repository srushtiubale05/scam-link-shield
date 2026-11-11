import { useEffect, useState } from "react";
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer } from "recharts";
import { request } from "./api";

function Card({ label, value, color }) {
  return (
    <div style={{ flex: 1, padding: 16, borderRadius: 10, background: "#f6f6f6" }}>
      <div style={{ fontSize: 32, fontWeight: 700, color }}>{value}</div>
      <div>{label}</div>
    </div>
  );
}

export default function Stats() {
  const [stats, setStats] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    request("/api/stats").then(setStats).catch((e) => setError(e.message));
  }, []);

  if (error) return <p className="err">{error}</p>;
  if (!stats) return <p>Loading...</p>;

  const chart = Object.entries(stats.byCategory).map(([name, count]) => ({
    name: name.replace("_", " "), count,
  }));

  return (
    <>
      <div style={{ display: "flex", gap: 16, margin: "16px 0" }}>
        <Card label="Verified scams" value={stats.verified} color="#b3261e" />
        <Card label="Pending review" value={stats.pending} color="#b26a00" />
        <Card label="Rejected" value={stats.rejected} color="#666" />
      </div>
      <h2>Scams by category</h2>
      <div style={{ height: 260 }}>
        <ResponsiveContainer>
          <BarChart data={chart}>
            <XAxis dataKey="name" /><YAxis allowDecimals={false} /><Tooltip />
            <Bar dataKey="count" fill="#b3261e" />
          </BarChart>
        </ResponsiveContainer>
      </div>
      <h2>Recently verified</h2>
      <table>
        <thead><tr><th>Link</th><th>Category</th><th>Confirmations</th></tr></thead>
        <tbody>
          {stats.recent.map((r) => (
            <tr key={r.id}>
              <td className="mono">{r.normalizedUrl}</td>
              <td>{r.category.replace("_", " ")}</td>
              <td>{r.confirmationCount}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </>
  );
}