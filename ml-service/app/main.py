import json
from pathlib import Path

import joblib
from fastapi import FastAPI
from pydantic import BaseModel

from app.features import extract

BASE = Path(__file__).resolve().parent.parent
model = joblib.load(BASE / "model.joblib")

HIGH, MEDIUM = 0.9, 0.7

app = FastAPI(title="Scam Link Shield ML service")


class ScoreRequest(BaseModel):
    url: str  # normalized URL, same form the Java backend produces


class ScoreResponse(BaseModel):
    score: int       # 0-100
    level: str       # HIGH_RISK | SUSPICIOUS | NO_SIGNAL


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/score", response_model=ScoreResponse)
def score(req: ScoreRequest):
    p = float(model.predict_proba([extract(req.url)])[0][1])
    level = "HIGH_RISK" if p >= HIGH else "SUSPICIOUS" if p >= MEDIUM else "NO_SIGNAL"
    return ScoreResponse(score=round(p * 100), level=level)