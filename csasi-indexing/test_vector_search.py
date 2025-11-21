#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
벡터 검색 테스트 스크립트
"""

import requests
from elasticsearch import Elasticsearch

# 설정
ES_HOST = 'http://localhost:9200'
EMBEDDING_API_URL = 'http://localhost:5001/embed'
INDEX_NAME = 'csasi_consultation'

es = Elasticsearch([ES_HOST])

def get_embedding(text):
    """텍스트를 벡터로 변환"""
    response = requests.post(
        EMBEDDING_API_URL,
        json={'text': text},
        timeout=30
    )
    return response.json()['vector']

def vector_search(query_text, top_k=5):
    """벡터 검색 수행"""
    print(f"🔍 검색어: '{query_text}'")
    print("📦 벡터 생성 중...")
    
    query_vector = get_embedding(query_text)
    print(f"✅ 벡터 생성 완료 (차원: {len(query_vector)})")
    
    print("🔎 벡터 검색 실행 중...")
    
    result = es.search(
        index=INDEX_NAME,
        body={
            "knn": {
                "field": "content_vector",
                "query_vector": query_vector,
                "k": top_k,
                "num_candidates": 100
            },
            "_source": ["csasi_id", "csasi_name", "browse_count", "properties"],
            "size": top_k
        }
    )
    
    print(f"\n📊 검색 결과: {result['hits']['total']['value']}건")
    print("=" * 80)
    
    for idx, hit in enumerate(result['hits']['hits'], 1):
        doc = hit['_source']
        score = hit['_score']
        
        print(f"\n{idx}. {doc['csasi_name']} (ID: {doc['csasi_id']})")
        print(f"   유사도: {score:.4f}")
        print(f"   조회수: {doc['browse_count']}")
        
        if doc['properties']:
            print(f"   가이드 내용 (미리보기):")
            for prop in doc['properties'][:2]:  # 처음 2개만
                content = prop['content'][:100] + "..." if len(prop['content']) > 100 else prop['content']
                print(f"     - {content}")
    
    print("\n" + "=" * 80)

if __name__ == '__main__':
    # 테스트 쿼리들
    queries = [
        "환불",
        "상품누락",
        "배송지연"
    ]
    
    for query in queries:
        vector_search(query, top_k=3)
        print("\n")

