#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
CSASI 상담 가이드 Elasticsearch 인덱스 생성 스크립트
"""

from elasticsearch import Elasticsearch

# Elasticsearch 연결
es = Elasticsearch(['http://localhost:9200'])

# 인덱스 이름
INDEX_NAME = 'csasi_consultation'

# 인덱스 매핑 정의
index_mapping = {
    "settings": {
        "number_of_shards": 1,
        "number_of_replicas": 0,
        "analysis": {
            "analyzer": {
                "my_nori_analyzer": {
                    "type": "custom",
                    "tokenizer": "nori_tokenizer",
                    "filter": ["lowercase", "nori_part_of_speech"]
                }
            }
        }
    },
    "mappings": {
        "properties": {
            # 기본 정보
            "csasi_id": {
                "type": "keyword"
            },
            "csasi_name": {
                "type": "text",
                "analyzer": "my_nori_analyzer",
                "fields": {
                    "keyword": {
                        "type": "keyword"
                    }
                }
            },
            "browse_count": {
                "type": "integer"
            },
            
            # 속성 정보 (배열)
            "properties": {
                "type": "nested",
                "properties": {
                    "prop_id": {
                        "type": "keyword"
                    },
                    "prop_type_cd": {
                        "type": "keyword"
                    },
                    "prop_seq": {
                        "type": "integer"
                    },
                    "content": {
                        "type": "text",
                        "analyzer": "my_nori_analyzer"
                    }
                }
            },
            
            # 전체 내용 (검색용)
            "full_content": {
                "type": "text",
                "analyzer": "my_nori_analyzer"
            },
            
            # 벡터 필드 (768 차원)
            "content_vector": {
                "type": "dense_vector",
                "dims": 768,
                "index": True,
                "similarity": "cosine"
            },
            
            # 메타데이터
            "use_yn": {
                "type": "keyword"
            },
            "reg_dts": {
                "type": "date",
                "format": "yyyy-MM-dd HH:mm:ss"
            },
            "indexed_at": {
                "type": "date"
            }
        }
    }
}

def create_index():
    """인덱스 생성"""
    
    try:
        # 기존 인덱스 삭제 (있다면)
        if es.indices.exists(index=INDEX_NAME):
            print(f"❌ 기존 인덱스 '{INDEX_NAME}' 삭제 중...")
            es.indices.delete(index=INDEX_NAME)
            print("✅ 삭제 완료")
    except Exception as e:
        print(f"⚠️  인덱스 존재 확인 중 오류 (무시): {e}")
    
    # 새 인덱스 생성
    print(f"🔨 인덱스 '{INDEX_NAME}' 생성 중...")
    es.indices.create(index=INDEX_NAME, **index_mapping)
    print("✅ 인덱스 생성 완료!")
    
    # 인덱스 정보 확인
    info = es.indices.get(index=INDEX_NAME)
    print(f"\n📊 인덱스 정보:")
    print(f"  - 샤드 수: {info[INDEX_NAME]['settings']['index']['number_of_shards']}")
    print(f"  - 레플리카 수: {info[INDEX_NAME]['settings']['index']['number_of_replicas']}")
    print(f"  - 벡터 차원: 768")
    print(f"  - 유사도 측정: cosine")

if __name__ == '__main__':
    create_index()

