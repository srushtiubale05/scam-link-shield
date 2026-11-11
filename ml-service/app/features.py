import math
import re
from collections import Counter

RISKY_TLDS = {"xyz", "top", "tk", "ml", "ga", "cf", "gq", "click", "link",
              "live", "icu", "buzz", "rest", "monster", "cyou", "cam", "work"}
KEYWORDS = ["kyc", "verify", "update", "login", "secure", "account", "bank",
            "reward", "lottery", "winner", "claim", "refund", "free", "bonus",
            "urgent", "blocked", "suspend", "otp", "upi", "paytm", "offer",
            "job", "registration", "fee", "gift", "prize"]
BRANDS = ["sbi", "hdfc", "icici", "axis", "kotak", "paytm", "phonepe", "gpay",
          "google", "amazon", "flipkart", "npci", "irctc", "aadhaar", "uidai",
          "whatsapp", "facebook", "instagram", "netflix", "microsoft", "apple"]
SHORTENERS = {"bit.ly", "tinyurl.com", "t.co", "goo.gl", "is.gd", "cutt.ly", "rb.gy"}
SECOND_LEVEL = {"co", "com", "org", "net", "gov", "ac", "nic", "edu"}

FEATURE_NAMES = [
    "url_len", "host_len", "path_len", "query_len", "path_depth", "n_params",
    "host_dots", "host_hyphens", "host_digits", "host_digit_ratio", "n_subdomains",
    "is_ip", "has_at", "has_punycode", "pct_encoded", "longest_digit_run",
    "host_entropy", "path_entropy", "risky_tld", "is_shortener",
    "keyword_count", "brand_lookalike",
]


def entropy(s: str) -> float:
    if not s:
        return 0.0
    counts = Counter(s)
    n = len(s)
    return -sum(c / n * math.log2(c / n) for c in counts.values())


def split_url(normalized: str):
    """normalized form: host[:port][/path][?query]"""
    rest = normalized
    query = ""
    if "?" in rest:
        rest, query = rest.split("?", 1)
    path = ""
    if "/" in rest:
        host, path = rest.split("/", 1)
        path = "/" + path
    else:
        host = rest
    host = host.split(":")[0]
    return host, path, query


def registered_label(host: str) -> str:
    parts = host.split(".")
    if len(parts) >= 3 and parts[-2] in SECOND_LEVEL:
        return parts[-3]
    if len(parts) >= 2:
        return parts[-2]
    return host


def extract(normalized: str) -> list[float]:
    url = normalized.lower()
    host, path, query = split_url(url)
    labels = host.split(".")
    tld = labels[-1] if labels else ""
    reg = registered_label(host)

    digits = sum(ch.isdigit() for ch in host)
    runs = re.findall(r"\d+", url)
    is_ip = 1 if re.fullmatch(r"\d{1,3}(\.\d{1,3}){3}", host) else 0
    keyword_count = sum(1 for k in KEYWORDS if k in url)
    # brand appears in the host, but the real registered domain is not just the brand
    lookalike = int(any(b in host and reg != b for b in BRANDS))

    return [
        len(url), len(host), len(path), len(query),
        path.count("/"), (query.count("&") + 1) if query else 0,
        host.count("."), host.count("-"), digits, digits / max(1, len(host)),
        max(0, len(labels) - 2),
        is_ip, int("@" in url), int("xn--" in host), url.count("%"),
        max((len(r) for r in runs), default=0),
        entropy(host), entropy(path),
        int(tld in RISKY_TLDS), int(host in SHORTENERS),
        keyword_count, lookalike,
    ]