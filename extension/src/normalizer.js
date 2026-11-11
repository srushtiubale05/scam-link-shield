const TRACKING = new Set(["fbclid", "gclid", "msclkid", "igshid", "yclid", "mc_cid", "mc_eid"]);

function isTracking(param) {
  const i = param.indexOf("=");
  const name = (i >= 0 ? param.slice(0, i) : param).toLowerCase();
  return name.startsWith("utm_") || TRACKING.has(name);
}

export function normalize(raw) {
  if (raw == null || String(raw).trim() === "") throw new Error("URL is empty");
  let s = String(raw).trim();
  if (!s.includes("://")) s = "http://" + s;

  let u;
  try { u = new URL(s); } catch { throw new Error("Invalid URL"); }
  if (u.protocol !== "http:" && u.protocol !== "https:") {
    throw new Error("Only http and https URLs are supported");
  }

  let host = u.hostname.toLowerCase();          // URL already converts to punycode
  if (host.startsWith("www.")) host = host.slice(4);

  let path = u.pathname.replace(/\/{2,}/g, "/");
  if (path.endsWith("/")) path = path.slice(0, -1);

  let query = "";
  if (u.search.length > 1) {
    query = u.search.slice(1).split("&")
      .filter(p => p !== "" && !isTracking(p))
      .sort()
      .join("&");
  }

  const port = u.port ? ":" + u.port : "";     // empty for default ports
  const normalizedUrl = host + port + path + (query ? "?" + query : "");
  return { normalizedUrl, domain: host };
}