import { CATEGORIES, getAuth, login, logout, reportUrl } from "./src/api.js";

const $ = (id) => document.getElementById(id);
let currentUrl = "";

function say(text, ok = false) {
  const el = $("msg");
  el.textContent = text;
  el.className = ok ? "ok" : "err";
}

async function render() {
  const auth = await getAuth();
  $("loginView").classList.toggle("hidden", !!auth);
  $("mainView").classList.toggle("hidden", !auth);
  if (auth) {
    $("who").textContent = auth.name;
    const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
    currentUrl = tab?.url || "";
    $("pageUrl").textContent = currentUrl;
  }
}

$("category").innerHTML = "";
for (const [value, label] of CATEGORIES) {
  const o = document.createElement("option");
  o.value = value;
  o.textContent = label;
  $("category").appendChild(o);
}

$("loginBtn").addEventListener("click", async () => {
  say("");
  try {
    await login($("email").value.trim(), $("password").value);
    $("password").value = "";
    await render();
  } catch (e) {
    say(e.message);
  }
});

$("logoutBtn").addEventListener("click", async () => {
  await logout();
  say("");
  await render();
});

$("reportBtn").addEventListener("click", async () => {
  say("Sending...", true);
  const r = await reportUrl(currentUrl, $("category").value);
  say(r.message, r.ok);
  if (!(await getAuth())) await render();
});

render();