const params = new URLSearchParams(location.search);
const target = params.get("url") || "";
const category = params.get("cat") || "SCAM";
const confirmations = params.get("n") || "?";
const norm = params.get("norm") || "";
const src = params.get("src") || "COMMUNITY";

document.getElementById("url").textContent = target;
document.getElementById("meta").textContent =
  src !== "COMMUNITY"
    ? `Category: ${category.replace("_", " ")} · listed by ${src}`
    : `Category: ${category.replace("_", " ")} · confirmed by ${confirmations} user(s)`;

document.getElementById("back").addEventListener("click", () => {
  if (history.length > 1) history.back();
  else chrome.tabs.update({ url: "chrome://newtab" });
});

document.getElementById("proceed").addEventListener("click", async () => {
  if (!/^https?:\/\//.test(target)) return;          // never navigate to anything but http(s)
  const { allow = {} } = await chrome.storage.local.get("allow");
  allow[norm] = Date.now() + 30 * 60 * 1000;          // allowed for 30 minutes
  await chrome.storage.local.set({ allow });
  location.href = target;
});