import { useEffect, useState } from "react";
import { request } from "./api";

export default function Admin() {
  const [pending, setPending] = useState([]);
  const [error, setError] = useState("");

  const load = () =>
    request("/api/admin/reports?status=PENDING").then(setPending).catch((e) => setError(e.message));
  useEffect(() => { load(); }, []);

  async function act(id, action) {
    try {
      await request(`/api/admin/reports/${id}/${action}`, { method: "POST" });
      load();
    } catch (e) {
      setError(e.message);
    }
  }

  return (
    <>
      <h2>Pending reports</h2>
      {error && <p className="err">{error}</p>}
      {pending.length === 0 && <p>Nothing waiting for review.</p>}
      <table>
        <tbody>
          {pending.map((r) => (
            <tr key={r.id}>
              <td className="mono">{r.normalizedUrl}</td>
              <td>{r.category.replace("_", " ")}</td>
              <td>{r.confirmationCount} confirmation(s)</td>
              <td>
                <button className="ok" onClick={() => act(r.id, "approve")}>Approve</button>{" "}
                <button onClick={() => act(r.id, "reject")}>Reject</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </>
  );
}