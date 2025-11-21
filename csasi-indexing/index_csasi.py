#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
CSASI 상담 가이드 데이터 인덱싱 스크립트
Oracle DB → Embedding → Elasticsearch
"""

import oracledb
import requests
from elasticsearch import Elasticsearch
from elasticsearch.helpers import bulk
from datetime import datetime
import os
import re

# 설정
ORACLE_CONFIG = {
    'user': 'DEVSSG',
    'password': 'd2vssg12#',
    'host': '10.203.7.71',
    'port': 1538,
    'service_name': 'DEVUTFDB'
}

ES_HOST = 'http://localhost:9200'
EMBEDDING_API_URL = 'http://localhost:5001/embed'
INDEX_NAME = 'csasi_consultation'

# Elasticsearch 연결
es = Elasticsearch([ES_HOST])

def clean_html(text):
    """HTML 태그 제거 및 텍스트 정리"""
    if not text:
        return ""
    
    # HTML 태그 제거
    text = re.sub(r'<[^>]+>', ' ', text)
    # HTML 엔티티 변환
    text = text.replace('&nbsp;', ' ')
    text = text.replace('&lt;', '<')
    text = text.replace('&gt;', '>')
    text = text.replace('&amp;', '&')
    text = text.replace('&quot;', '"')
    # 연속된 공백 제거
    text = re.sub(r'\s+', ' ', text)
    # 앞뒤 공백 제거
    text = text.strip()
    
    return text

def get_embedding(text):
    """텍스트를 벡터로 변환"""
    try:
        response = requests.post(
            EMBEDDING_API_URL,
            json={'text': text},
            timeout=30
        )
        response.raise_for_status()
        return response.json()['vector']
    except Exception as e:
        print(f"❌ 벡터 생성 실패: {e}")
        return None

def fetch_csasi_data():
    """Oracle에서 CSASI 데이터 조회"""
    print("📊 Oracle DB에서 데이터 조회 중...")
    
    connection = oracledb.connect(**ORACLE_CONFIG)
    cursor = connection.cursor()
    
    # CSASI 데이터 조회
    sql = """
    SELECT 
        c.CSASI_ID,
        c.CSASI_NM,
        c.CSASI_BRWS_CNT,
        c.USE_YN,
        c.REG_DTS,
        p.CSASI_PROP_ID,
        p.CSASI_PROP_TYPE_CD,
        pc.CSASI_PROP_SEQ,
        pc.CSASI_PROP_CNTT
    FROM SSG.CSASI c
    LEFT JOIN SSG.CSASI_PROP p ON c.CSASI_ID = p.CSASI_ID
    LEFT JOIN SSG.CSASI_PROP_CNTT pc ON p.CSASI_PROP_ID = pc.CSASI_PROP_ID
    WHERE c.USE_YN = 'Y'
    ORDER BY c.CSASI_ID, p.CSASI_PROP_ID, pc.CSASI_PROP_SEQ
    """
    
    cursor.execute(sql)
    rows = cursor.fetchall()
    
    cursor.close()
    connection.close()
    
    print(f"✅ {len(rows)}건의 데이터 조회 완료")
    return rows

def group_csasi_data(rows):
    """CSASI 데이터를 그룹화"""
    print("🔄 데이터 그룹화 중...")
    
    csasi_dict = {}
    
    for row in rows:
        (csasi_id, csasi_nm, brws_cnt, use_yn, reg_dts,
         prop_id, prop_type_cd, prop_seq, prop_cntt) = row
        
        # CSASI 기본 정보
        if csasi_id not in csasi_dict:
            csasi_dict[csasi_id] = {
                'csasi_id': csasi_id,
                'csasi_name': csasi_nm,
                'browse_count': brws_cnt if brws_cnt else 0,
                'use_yn': use_yn,
                'reg_dts': reg_dts.strftime('%Y-%m-%d %H:%M:%S') if reg_dts else None,
                'properties': []
            }
        
        # 속성 정보 추가
        if prop_id and prop_cntt:
            csasi_dict[csasi_id]['properties'].append({
                'prop_id': prop_id,
                'prop_type_cd': prop_type_cd,
                'prop_seq': prop_seq,
                'content': clean_html(prop_cntt)
            })
    
    csasi_list = list(csasi_dict.values())
    print(f"✅ {len(csasi_list)}개의 상담 가이드로 그룹화 완료")
    return csasi_list

def create_documents(csasi_list):
    """Elasticsearch 문서 생성 (벡터화 포함)"""
    print("🔨 Elasticsearch 문서 생성 중...")
    
    documents = []
    total = len(csasi_list)
    
    for idx, csasi in enumerate(csasi_list, 1):
        # 전체 내용 생성 (벡터화용)
        full_content_parts = [csasi['csasi_name']]
        
        for prop in csasi['properties']:
            if prop['content']:
                full_content_parts.append(prop['content'])
        
        full_content = ' '.join(full_content_parts)
        
        # 벡터 생성
        print(f"  [{idx}/{total}] {csasi['csasi_id']}: {csasi['csasi_name'][:30]}... 벡터화 중...", end='')
        
        vector = get_embedding(full_content)
        
        if vector is None:
            print(" ❌ 실패")
            continue
        
        print(" ✅")
        
        # Elasticsearch 문서 생성
        doc = {
            '_index': INDEX_NAME,
            '_id': csasi['csasi_id'],
            'csasi_id': csasi['csasi_id'],
            'csasi_name': csasi['csasi_name'],
            'browse_count': csasi['browse_count'],
            'properties': csasi['properties'],
            'full_content': full_content,
            'content_vector': vector,
            'use_yn': csasi['use_yn'],
            'reg_dts': csasi['reg_dts'],
            'indexed_at': datetime.now().isoformat()
        }
        
        documents.append(doc)
    
    print(f"✅ {len(documents)}개 문서 생성 완료")
    return documents

def index_documents(documents):
    """Elasticsearch에 문서 인덱싱"""
    print("📤 Elasticsearch에 데이터 인덱싱 중...")
    
    success, failed = bulk(es, documents, raise_on_error=False)
    
    print(f"✅ 인덱싱 완료: 성공 {success}건, 실패 {len(failed)}건")
    
    if failed:
        print("❌ 실패한 문서:")
        for item in failed[:5]:  # 처음 5개만 출력
            print(f"  - {item}")
    
    return success, failed

def verify_indexing():
    """인덱싱 결과 확인"""
    print("\n🔍 인덱싱 결과 확인 중...")
    
    # 인덱스 통계
    stats = es.indices.stats(index=INDEX_NAME)
    doc_count = stats['indices'][INDEX_NAME]['total']['docs']['count']
    
    print(f"✅ 총 문서 수: {doc_count}건")
    
    # 샘플 문서 조회
    result = es.search(
        index=INDEX_NAME,
        body={
            "size": 3,
            "query": {"match_all": {}},
            "sort": [{"browse_count": {"order": "desc"}}]
        }
    )
    
    print(f"\n📄 인기 상담 가이드 TOP 3:")
    for hit in result['hits']['hits']:
        doc = hit['_source']
        print(f"  - {doc['csasi_name']} (조회수: {doc['browse_count']})")
        print(f"    속성 수: {len(doc['properties'])}개")
        print(f"    벡터 차원: {len(doc['content_vector'])}")

def main():
    """메인 함수"""
    print("=" * 60)
    print("🚀 CSASI 상담 가이드 인덱싱 시작")
    print("=" * 60)
    
    try:
        # 1. 데이터 조회
        rows = fetch_csasi_data()
        
        # 2. 데이터 그룹화
        csasi_list = group_csasi_data(rows)
        
        # 3. 문서 생성 (벡터화)
        documents = create_documents(csasi_list)
        
        # 4. 인덱싱
        success, failed = index_documents(documents)
        
        # 5. 검증
        verify_indexing()
        
        print("\n" + "=" * 60)
        print("✅ 인덱싱 완료!")
        print("=" * 60)
        
    except Exception as e:
        print(f"\n❌ 오류 발생: {e}")
        import traceback
        traceback.print_exc()

if __name__ == '__main__':
    main()

