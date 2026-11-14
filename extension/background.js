import { BloomFilter } from "./src/bloom.js";
import { normalize } from "./src/normalizer.js";
import { API, CATEGORIES, reportUrl } from "./src/api.js";

let filter = null;

// 1x1 red PNG, used as the notification icon so we don't need an image file
const ICON =
  "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";

function notify(title, message) {
  chrome.notifications.create({ type: "basic", iconUrl: ICON, title, message });
}

async function loadFromStorage() {
  const { bloom } = await chrome.storage.local.get("bloom");
  if (bloom) {
    filter = BloomFilter.fromBase64(bloom.data, bloom.m, bloom.k);
    console.log("Loaded filter from storage, items:", bloom.count);
  }
}

async function refreshFilter() {
  try {
    const ver = await (await fetch(`${API}/api/bloom/version`)).json();
    const { bloom } = await chrome.storage.local.get("bloom");
    if (bloom && bloom.version === ver.version) {
      console.log("Filter already up to date, items:", bloom.count);
      return;
    }
    const snap = await (await fetch(`${API}/api/bloom`)).json();
    await chrome.storage.local.set({ bloom: snap });
    filter = BloomFilter.fromBase64(snap.data, snap.m, snap.k);
    console.log("Bloom filter updated, items:", snap.count);
  } catch (e) {
    console.warn("Could not refresh filter (using the stored one):", e.message);
  }
}

async function ready() {
  if (!filter) await loadFromStorage();
  if (!filter) await refreshFilter();
}

async function isAllowed(norm) {
  const { allow = {} } = await chrome.storage.local.get("allow");
  const expiry = allow[norm];
  if (!expiry) return false;
  if (expiry < Date.now()) {
    delete allow[norm];
    await chrome.storage.local.set({ allow });
    return false;
  }
  return true;
}

function createMenus() {
  chrome.contextMenus.removeAll(() => {
    chrome.contextMenus.create({
      id: "report",
      title: "Report as scam",
      contexts: ["link", "page"],
    });
    for (const [value, label] of CATEGORIES) {
      chrome.contextMenus.create({
        id: `report:${value}`,
        parentId: "report",
        title: label,
        contexts: ["link", "page"],
      });
    }
  });
}

chrome.runtime.onInstalled.addListener(() => {
  chrome.alarms.create("refresh", { periodInMinutes: 30 });
  createMenus();
  refreshFilter();
});

chrome.alarms.onAlarm.addListener((a) => {
  if (a.name === "refresh") refreshFilter();
});

chrome.contextMenus.onClicked.addListener(async (info) => {
  if (!String(info.menuItemId).startsWith("report:")) return;
  const category = String(info.menuItemId).split(":")[1];
  const url = info.linkUrl || info.pageUrl;
  const r = await reportUrl(url, category);
  notify(r.ok ? "Reported" : "Could not report", r.message);
});

chrome.webNavigation.onCommitted.addListener(async (d) => {
  if (d.frameId !== 0 || !/^https?:/.test(d.url)) return;
  await ready();
  if (!filter) return;

  let norm;
  try {
    norm = normalize(d.url).normalizedUrl;
  } catch {
    return;
  }

  const possible = filter.mightContain(norm);
  console.log("Checking:", norm, "| possible match:", possible);
  if (!possible) return;

  if (await isAllowed(norm)) {
    console.log("User chose to proceed, skipping warning:", norm);
    return;
  }

  let res;
  try {
    res = await (await fetch(`${API}/api/check?url=${encodeURIComponent(d.url)}`)).json();
  } catch (e) {
    console.warn("Backend unreachable:", e.message);
    return;
  }

  if (res.verdict === "KNOWN_SCAM") {
    console.warn("SCAM BLOCKED:", norm);
    const q = new URLSearchParams({
      url: d.url,
      norm,
      cat: res.category || "SCAM",
      n: String(res.confirmations ?? "?"),
      src: res.source || "COMMUNITY",
    });
    chrome.tabs.update(d.tabId, { url: chrome.runtime.getURL("warning.html") + "?" + q });
  } else {
    console.log("Backend says not a known scam (false positive):", res.verdict);
  }
});

console.log("Scam Link Shield worker started");
ready().then(refreshFilter);