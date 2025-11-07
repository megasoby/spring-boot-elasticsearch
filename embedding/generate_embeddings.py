#!/usr/bin/env python3
"""
Elasticsearch 상품 데이터 벡터화 스크립트

products_korean 인덱스의 모든 상품을 읽어서
sentence-transformers로 벡터를 생성하고
name_vector 필드에 저장합니다.
"""

from sentence_transformers import SentenceTransformer
from elasticsearch import Elasticsearch
import sys

# 설정
ES_HOST = "http://localhost:9200"
INDEX_NAME = "products_korean"
MODEL_NAME = "jhgan/ko-sroberta-multitask"  # 한글 전용 모델

def main():
    print("=" * 60)
    print("🚀 Elasticsearch 상품 벡터화 시작")
    print("=" * 60)
    
    # 1. Elasticsearch 연결
    print(f"\n📡 Elasticsearch 연결 중... ({ES_HOST})")
    try:
        es = Elasticsearch(ES_HOST, verify_certs=False)
        print("✅ Elasticsearch 클라이언트 생성 완료")
        
        # Ping 테스트
        ping_result = es.ping()
        print(f"   Ping 결과: {ping_result}")
        
        if not ping_result:
            print("❌ Elasticsearch 연결 실패!")
            sys.exit(1)
        print("✅ Elasticsearch 연결 성공!")
    except Exception as e:
        print(f"❌ 연결 오류: {type(e).__name__}: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
    
    # 2. sentence-transformers 모델 로드
    print(f"\n🤖 모델 로드 중... ({MODEL_NAME})")
    print("   (처음 실행 시 모델 다운로드로 시간이 걸릴 수 있습니다)")
    try:
        model = SentenceTransformer(MODEL_NAME)
        print("✅ 모델 로드 완료!")
    except Exception as e:
        print(f"❌ 모델 로드 실패: {e}")
        sys.exit(1)
    
    # 3. 모든 상품 가져오기
    print(f"\n📦 '{INDEX_NAME}' 인덱스에서 상품 조회 중...")
    try:
        response = es.search(
            index=INDEX_NAME,
            query={"match_all": {}},
            size=10000,  # 최대 10000개
            _source=["id", "name", "description"]
        )
        
        products = response['hits']['hits']
        total = len(products)
        
        if total == 0:
            print(f"⚠️  '{INDEX_NAME}' 인덱스에 상품이 없습니다!")
            sys.exit(1)
            
        print(f"✅ {total}개 상품 조회 완료!")
        
    except Exception as e:
        print(f"❌ 상품 조회 실패: {e}")
        sys.exit(1)
    
    # 4. 각 상품에 대해 벡터 생성 및 저장
    print(f"\n🔄 벡터 생성 및 저장 중...")
    print("-" * 60)
    
    success_count = 0
    error_count = 0
    
    for idx, hit in enumerate(products, 1):
        product_id = hit['_id']
        product_name = hit['_source'].get('name', '')
        
        if not product_name:
            print(f"⚠️  상품 ID {product_id}: 이름이 없어 스킵합니다")
            error_count += 1
            continue
        
        try:
            # 벡터 생성
            vector = model.encode(product_name).tolist()
            
            # Elasticsearch에 벡터 저장
            es.update(
                index=INDEX_NAME,
                id=product_id,
                body={
                    "doc": {
                        "name_vector": vector
                    }
                }
            )
            
            success_count += 1
            print(f"[{idx}/{total}] ✅ {product_name[:30]:<30} → 벡터 저장 완료")
            
        except Exception as e:
            error_count += 1
            print(f"[{idx}/{total}] ❌ {product_name[:30]:<30} → 오류: {e}")
    
    # 5. 결과 출력
    print("-" * 60)
    print(f"\n✨ 벡터화 완료!")
    print(f"   성공: {success_count}개")
    print(f"   실패: {error_count}개")
    print(f"   총합: {total}개")
    print("=" * 60)

if __name__ == "__main__":
    main()

