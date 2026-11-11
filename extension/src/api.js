export const API = "http://localhost:8080";

export const CATEGORIES = [
  ["PHISHING", "Phishing"],
  ["FAKE_JOB", "Fake job"],
  ["UPI_FRAUD", "UPI fraud"],
  ["LOTTERY_SCAM", "Lottery scam"],
  ["OTHER", "Other"],
];

export async function getAuth() {
  const { auth } = await chrome.storage.local.get("auth");
  return auth || null;
}

export async function logout() {
  await chrome.storage.local.remove("auth");
}

export async function login(email, password) {
  const res = await fetch(`${API}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
  });
  const body = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(body.message || "Login failed");
  await chrome.storage.local.set({
    auth: { token: body.token, name: body.name, email: body.email, role: body.role },
  });
  return body;
}

/** Returns { ok, message }. Never throws, so callers can just show the message. */
export async function reportUrl(url, category) {
  if (!/^https?:\/\//i.test(url || "")) {
    return { ok: false, message: "Only http and https links can be reported." };
  }
  const auth = await getAuth();
  if (!auth) return { ok: false, message: "Please log in from the extension popup first." };

  let res;
  try {
    res = await fetch(`${API}/api/reports`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${auth.token}` },
      body: JSON.stringify({ url, category }),
    });
  } catch {
    return { ok: false, message: "Cannot reach the server." };
  }

  const body = await res.json().catch(() => ({}));
  if (res.status === 401 || res.status === 403) {
    await logout();                                   // token expired
    return { ok: false, message: "Session expired. Please log in again." };
  }
  if (!res.ok) return { ok: false, message: body.message || `Failed (${res.status})` };
  return {
    ok: true,
    message: `Thanks! Confirmations: ${body.confirmationCount}, status: ${body.status}`,
  };
}