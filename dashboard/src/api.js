export const API = import.meta.env.VITE_API_URL || "http://localhost:8080";

export function getAuth() {
  try { return JSON.parse(localStorage.getItem("auth")); } catch { return null; }
}
export function setAuth(a) {
  if (a) localStorage.setItem("auth", JSON.stringify(a));
  else localStorage.removeItem("auth");
}

export async function request(path, { method = "GET", body } = {}) {
  const auth = getAuth();
  const headers = { "Content-Type": "application/json" };
  if (auth) headers.Authorization = `Bearer ${auth.token}`;

  let res;
  try {
    res = await fetch(API + path, { method, headers, body: body ? JSON.stringify(body) : undefined });
  } catch {
    throw new Error("Cannot reach the server");
  }
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    if (auth && res.status === 401) {          // token expired
      setAuth(null);
      window.location.reload();
    }
    throw new Error(data.message || `Request failed (${res.status})`);
  }
  return data;
}

export const CATEGORIES = ["PHISHING", "FAKE_JOB", "UPI_FRAUD", "LOTTERY_SCAM", "OTHER"];