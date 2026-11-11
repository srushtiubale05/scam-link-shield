import test from "node:test";
import assert from "node:assert/strict";
import { normalize } from "../src/normalizer.js";

const n = (u) => normalize(u).normalizedUrl;

test("lowercases host, strips www, tracking and fragment", () => {
  assert.equal(n("HTTPS://WWW.Scam-Site.XYZ/Login/?utm_source=wa&fbclid=abc#top"), "scam-site.xyz/Login");
});
test("adds scheme when missing", () => assert.equal(n("scam-site.xyz/login"), "scam-site.xyz/login"));
test("http and https match", () => assert.equal(n("http://scam-site.xyz/a"), n("https://scam-site.xyz/a")));
test("default and custom ports", () => {
  assert.equal(n("http://scam-site.xyz:80/a"), "scam-site.xyz/a");
  assert.equal(n("https://scam-site.xyz:8443/a"), "scam-site.xyz:8443/a");
});
test("sorts query params", () => assert.equal(n("http://x.com/p?b=2&a=1"), "x.com/p?a=1&b=2"));
test("root and trailing slashes", () => {
  assert.equal(n("http://x.com/"), "x.com");
  assert.equal(n("http://x.com//a//b/"), "x.com/a/b");
});
test("drops credentials", () => assert.equal(n("http://user:pw@x.com/a"), "x.com/a"));
test("extracts domain", () => assert.equal(normalize("https://www.scam-site.xyz/path").domain, "scam-site.xyz"));
test("rejects invalid input", () => {
  for (const bad of ["", "   ", null, "ftp://x.com/file", "not a url"]) {
    assert.throws(() => normalize(bad));
  }
});