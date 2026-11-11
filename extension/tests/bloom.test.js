import test from "node:test";
import assert from "node:assert/strict";
import { BloomFilter } from "../src/bloom.js";

const API = "http://localhost:8080";

test("JS reads the Java-built filter correctly", async (t) => {
  let snap;
  try {
    snap = await (await fetch(`${API}/api/bloom`)).json();
  } catch {
    t.skip("backend not running");
    return;
  }
  const f = BloomFilter.fromBase64(snap.data, snap.m, snap.k);
  console.log(`m=${f.m} k=${f.k} count=${snap.count} setBits=${f.bitCount()}`);

  // Both are VERIFIED in your database. If JS hashed differently, these would be false.
  assert.equal(f.mightContain("fake-sbi-kyc.xyz/login"), true);
  assert.equal(f.mightContain("fake-job-offer.in/apply"), true);

  // Unknown URLs should almost never match
  let hits = 0;
  for (let i = 0; i < 5000; i++) if (f.mightContain(`safe-${i}.com/home`)) hits++;
  assert.ok(hits / 5000 < 0.02, `too many false positives: ${hits}`);

  // set bits can't exceed count * k
  assert.ok(f.bitCount() > 0 && f.bitCount() <= snap.count * snap.k);
});