// Must match backend BloomFilter.java exactly: FNV-1a (32-bit), double hashing, LSB-first bits.

function fnv1a(bytes, seed) {
  let h = seed >>> 0;
  for (const b of bytes) {
    h ^= b;
    h = Math.imul(h, 0x01000193) >>> 0;   // 32-bit multiply, stays unsigned
  }
  return h >>> 0;
}

export class BloomFilter {
  constructor(m, k, bytes) {
    this.m = m;
    this.k = k;
    this.bytes = bytes;
  }

  static fromBase64(b64, m, k) {
    const bin = atob(b64);
    const bytes = new Uint8Array(bin.length);
    for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
    return new BloomFilter(m, k, bytes);
  }

  mightContain(item) {
    const data = new TextEncoder().encode(item);
    const h1 = fnv1a(data, 0x811C9DC5);
    const h2 = (fnv1a(data, (0x01000193 ^ 0x9E3779B9) >>> 0) | 1) >>> 0;
    for (let i = 0; i < this.k; i++) {
      const idx = (h1 + i * h2) % this.m;       // all values are non-negative and < 2^53
      const byteIndex = idx >> 3;
      // Java's BitSet trims trailing zero bytes, so a missing byte means 0
      const byte = byteIndex < this.bytes.length ? this.bytes[byteIndex] : 0;
      if (((byte >> (idx & 7)) & 1) === 0) return false;   // definitely not present
    }
    return true;                                            // probably present
  }

  bitCount() {
    let n = 0;
    for (const b of this.bytes) { let x = b; while (x) { n += x & 1; x >>= 1; } }
    return n;
  }
}