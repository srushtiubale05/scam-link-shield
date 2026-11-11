import { useEffect, useState } from "react";
import { request } from "./api";

export default function Admin({ notify }) {
  const [pending, setPending] = useState([]);
  const load = () =>
    request("/api/admin/reports?status=PENDING").then(setPending).catch((e) => notify(e.message, false));
  useEffect(() => { load(); }, []);

  async function act(id, action) {
    try {
      await request(`/api/admin/reports/${id}/${action}`, { method: "POST" });
      notify(action === "approve" ? "Approved ✅ now a verified scam" : "Rejected ❌");
      load();
    } catch (e) { notify(e.message, false); }
  }

  return (
    <div className="fade">
      <h2 className="sec">Pending reports ({pending.length})</h2>
      {pending.length === 0 && <div className="card">🎉 Nothing waiting for review.</div>}
      <div className="list">
        {pending.map((r) => (
          <div className="item" key={r.id}>
            <div className="grow mono">{r.normalizedUrl}</div>
            <span className="badge cat">{r.category.replace("_", " ")}</span>
            <span className="muted">{r.confirmationCount} confirmation(s)</span>
            <button className="btn ok small" onClick={() => act(r.id, "approve")}>Approve</button>
            <button className="btn bad small" onClick={() => act(r.id, "reject")}>Reject</button>
          </div>
        ))}
      </div>
    </div>
  );
}