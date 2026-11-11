import json
import re
import sys

import numpy as np
from sklearn.ensemble import HistGradientBoostingClassifier
from sklearn.metrics import roc_auc_score

import joblib
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import classification_report, confusion_matrix
from sklearn.model_selection import GroupShuffleSplit

from app.features import FEATURE_NAMES, extract, split_url

TRACKING = re.compile(r"(utm_[^&]*|fbclid=[^&]*|gclid=[^&]*)&?")


def normalize(raw: str) -> str:
    """Rough Python port of the Java UrlNormalizer (scheme, www, fragment, tracking removed)."""
    s = str(raw).strip().lower()
    s = re.sub(r"^[a-z]+://", "", s)
    s = s.split("#")[0]
    s = re.sub(r"^[^/@]*@", "", s)          # credentials
    s = re.sub(r"^www\.", "", s)
    if "?" in s:
        base, q = s.split("?", 1)
        q = TRACKING.sub("", q).strip("&")
        s = base + ("?" + q if q else "")
    return s.rstrip("/")


def load():
    frames = []
    for path in ("data/urls.csv", "data/indian_scams.csv"):
        try:
            df = pd.read_csv(path)
            frames.append(df[["URL", "Label"]])
        except FileNotFoundError:
            print("missing", path, file=sys.stderr)
    df = pd.concat(frames, ignore_index=True)
    df["y"] = (df["Label"].astype(str).str.lower().isin(["bad", "phishing", "1"])).astype(int)
    df["norm"] = df["URL"].map(normalize)
    df = df[df["norm"].str.len() > 3].drop_duplicates("norm")
    return df


def main():
    df = load()
    print("rows:", len(df), "| phishing share:", round(df["y"].mean(), 3))
    X = np.array([extract(u) for u in df["norm"]])
    y = df["y"].values
    groups = df["norm"].map(lambda u: split_url(u)[0])

    train_idx, test_idx = next(GroupShuffleSplit(test_size=0.2, random_state=42).split(X, y, groups))
    Xtr, Xte, ytr, yte = X[train_idx], X[test_idx], y[train_idx], y[test_idx]

    model = HistGradientBoostingClassifier(max_iter=300, learning_rate=0.1,
                                           class_weight="balanced", random_state=42)
    model.fit(Xtr, ytr)

    proba = model.predict_proba(Xte)[:, 1]
    print("ROC-AUC:", round(roc_auc_score(yte, proba), 4))
    print("threshold | precision | recall | false-positive rate")
    metrics = {}
    for t in (0.5, 0.7, 0.8, 0.9, 0.95):
        pred = (proba >= t).astype(int)
        tp = int(((pred == 1) & (yte == 1)).sum())
        fp = int(((pred == 1) & (yte == 0)).sum())
        fn = int(((pred == 0) & (yte == 1)).sum())
        tn = int(((pred == 0) & (yte == 0)).sum())
        prec = tp / max(1, tp + fp)
        rec = tp / max(1, tp + fn)
        fpr = fp / max(1, fp + tn)
        metrics[str(t)] = {"precision": prec, "recall": rec, "fpr": fpr}
        print(f"  {t:<8} | {prec:.3f}     | {rec:.3f}  | {fpr:.4f}")

    joblib.dump(model, "model.joblib", compress=3)
    with open("metrics.json", "w") as f:
        json.dump({"roc_auc": float(roc_auc_score(yte, proba)), "thresholds": metrics}, f, indent=2)
    print("saved model.joblib and metrics.json")


if __name__ == "__main__":
    main()