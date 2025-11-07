#!/usr/bin/env python3
"""
벡터 생성 API 서버

Spring Boot에서 호출하여 텍스트를 벡터로 변환합니다.
"""

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer
from typing import List
import uvicorn

# 설정
MODEL_NAME = "jhgan/ko-sroberta-multitask"
PORT = 5001

# FastAPI 앱
app = FastAPI(title="Embedding API", description="텍스트를 벡터로 변환하는 API")

# 전역 모델 (앱 시작 시 한 번만 로드)
model = None

@app.on_event("startup")
async def startup_event():
    """앱 시작 시 모델 로드"""
    global model
    print(f"🤖 모델 로드 중... ({MODEL_NAME})")
    model = SentenceTransformer(MODEL_NAME)
    print("✅ 모델 로드 완료!")

class EmbedRequest(BaseModel):
    """벡터 생성 요청"""
    text: str

class EmbedResponse(BaseModel):
    """벡터 생성 응답"""
    text: str
    vector: List[float]
    dimensions: int

@app.get("/")
async def root():
    """API 정보"""
    return {
        "service": "Embedding API",
        "model": MODEL_NAME,
        "status": "running"
    }

@app.get("/health")
async def health():
    """헬스 체크"""
    return {"status": "ok"}

@app.post("/embed", response_model=EmbedResponse)
async def embed(request: EmbedRequest):
    """텍스트를 벡터로 변환"""
    if not request.text or not request.text.strip():
        raise HTTPException(status_code=400, detail="텍스트가 비어있습니다")
    
    try:
        # 벡터 생성
        vector = model.encode(request.text).tolist()
        
        return EmbedResponse(
            text=request.text,
            vector=vector,
            dimensions=len(vector)
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"벡터 생성 오류: {str(e)}")

if __name__ == "__main__":
    print("=" * 60)
    print("🚀 Embedding API 서버 시작")
    print(f"   포트: {PORT}")
    print(f"   모델: {MODEL_NAME}")
    print("=" * 60)
    
    uvicorn.run(app, host="0.0.0.0", port=PORT)

