import { useEffect, useState } from "react";
import { request, CATEGORIES } from "./api";

export default function Reports() {
  const [url, setUrl] = useState("");
  const [category, setCategory] = useState(CATEGORIES[0]);
  const [msg, setMsg] = useState({ text: "", ok: true });
  const [mine, setMine] = useState([]);

  const load = () => request("/api/reports/mine").then(setMine).catch(() => {});
  useEffect(() => { load(); }, []);

  async function submit(e) {
    e.preventDefault();
    try {
      const r = await request("/api/reports", { method: "POST", body: { url, category } });
      setMsg({ text: `Thanks! Confirmations: ${r.confirmationCount}, status: ${r.status}`, ok: true });
      setUrl("");
      load();
    } catch (err) {
      setMsg({ text: err.message, ok: false });
    }
  }

  return (
    <>
      <h2>Report a scam link</h2>
      <form onSubmit={submit} style={{ display: "grid", maxWidth: 480 }}>
        <input placeholder="https://suspicious-link.xyz/login" value={url}
               onChange={(e) => setUrl(e.target.value)} required />
        <select value={category} onChange={(e) => setCategory(e.target.value)}>
          {CATEGORIES.map((c) => <option key={c} value={c}>{c.replace("_", " ")}</option>)}
        </select>
        <button type="submit">Submit report</button>
        {msg.text && <p className={msg.ok ? "good" : "err"}>{msg.text}</p>}
      </form>

      <h2>My reports</h2>
      <table>
        <thead><tr><th>Link</th><th>Category</th><th>Status</th><th>Confirmations</th></tr></thead>
        <tbody>
          {mine.map((r) => (
            <tr key={r.id}>
              <td className="mono">{r.normalizedUrl}</td>
              <td>{r.category.replace("_", " ")}</td>
              <td>{r.status}</td>
              <td>{r.confirmationCount}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </>
  );
}