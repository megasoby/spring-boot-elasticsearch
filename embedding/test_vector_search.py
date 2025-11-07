#!/usr/bin/env python3
"""
벡터 검색 테스트 스크립트
"""

from sentence_transformers import SentenceTransformer
from elasticsearch import Elasticsearch
import sys

# 설정
ES_HOST = "http://localhost:9200"
INDEX_NAME = "products_korean"
MODEL_NAME = "jhgan/ko-sroberta-multitask"

def vector_search(query_text, top_k=5):
    """벡터 기반 검색"""
    
    print("=" * 60)
    print(f"🔍 벡터 검색: '{query_text}'")
    print("=" * 60)
    
    # 1. ES 연결
    es = Elasticsearch(ES_HOST, verify_certs=False)
    
    # 2. 모델 로드
    print("\n🤖 모델 로드 중...")
    model = SentenceTransformer(MODEL_NAME)
    
    # 3. 검색어를 벡터로 변환
    print(f"\n📝 '{query_text}' → 벡터 변환 중...")
    query_vector = model.encode(query_text).tolist()
    print(f"✅ 벡터 생성 완료 (768차원)")
    
    # 4. kNN 검색
    print(f"\n🔎 유사한 상품 검색 중... (Top {top_k})")
    response = es.search(
        index=INDEX_NAME,
        knn={
            "field": "name_vector",
            "query_vector": query_vector,
            "k": top_k,
            "num_candidates": 100
        },
        _source=["name", "description", "price", "category"]
    )
    
    # 5. 결과 출력
    print("\n" + "=" * 60)
    print("📊 검색 결과")
    print("=" * 60)
    
    hits = response['hits']['hits']
    if not hits:
        print("❌ 검색 결과가 없습니다")
        return
    
    for idx, hit in enumerate(hits, 1):
        source = hit['_source']
        score = hit['_score']
        
        print(f"\n[{idx}] {source['name']}")
        print(f"    유사도 점수: {score:.4f}")
        print(f"    카테고리: {source['category']}")
        print(f"    가격: ₩{source['price']:,}")
        print(f"    설명: {source['description'][:50]}...")
    
    print("\n" + "=" * 60)

if __name__ == "__main__":
    # 테스트 검색어들
    test_queries = [
        "스마트폰",
        "컴퓨터",
        "의자",
        "무선"
    ]
    
    for query in test_queries:
        vector_search(query, top_k=3)
        print("\n\n")

