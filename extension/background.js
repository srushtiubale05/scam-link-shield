import { BloomFilter } from "./src/bloom.js";
import { normalize } from "./src/normalizer.js";

const API = "http://localhost:8080";
let filter = null;

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

chrome.runtime.onInstalled.addListener(() => {
  chrome.alarms.create("refresh", { periodInMinutes: 30 });
  refreshFilter();
});

chrome.alarms.onAlarm.addListener((a) => {
  if (a.name === "refresh") refreshFilter();
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
  if (!possible) return;                       // definitely not in the list

  // Possible match: confirm with the backend to rule out a false positive
  let res;
  try {
    res = await (await fetch(`${API}/api/check?url=${encodeURIComponent(d.url)}`)).json();
  } catch (e) {
    console.warn("Backend unreachable:", e.message);
    return;
  }

  if (res.verdict === "KNOWN_SCAM") {
    console.warn("SCAM BLOCKED:", norm);
    chrome.action.setBadgeText({ tabId: d.tabId, text: "!" });
    chrome.action.setBadgeBackgroundColor({ tabId: d.tabId, color: "#d93025" });
  } else {
    console.log("Backend says not a known scam (false positive):", res.verdict);
  }
});

console.log("Scam Link Shield worker started");
ready().then(refreshFilter);