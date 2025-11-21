#!/bin/bash

echo "=========================================="
echo "🚀 CSASI 인덱싱 실행"
echo "=========================================="

# 현재 디렉토리로 이동
cd "$(dirname "$0")"

# 가상환경 활성화
source venv/bin/activate

echo ""
echo "📋 사전 확인:"
echo ""

# Elasticsearch 확인
echo "1. Elasticsearch 상태 확인..."
if curl -s http://localhost:9200 > /dev/null; then
    echo "   ✅ Elasticsearch 실행 중"
else
    echo "   ❌ Elasticsearch 연결 실패!"
    echo "      → Elasticsearch를 먼저 실행해주세요"
    exit 1
fi

# Python Embedding API 확인
echo "2. Python Embedding API 상태 확인..."
if curl -s http://localhost:5001/health > /dev/null; then
    echo "   ✅ Embedding API 실행 중"
else
    echo "   ❌ Embedding API 연결 실패!"
    echo "      → Python Embedding API를 먼저 실행해주세요"
    echo "      → cd ../embedding && source venv/bin/activate && uvicorn embedding_api:app --port 5001"
    exit 1
fi

echo ""
echo "=========================================="
echo "📊 Step 1: 인덱스 생성"
echo "=========================================="
python create_index.py

if [ $? -ne 0 ]; then
    echo "❌ 인덱스 생성 실패!"
    exit 1
fi

echo ""
echo "=========================================="
echo "📤 Step 2: 데이터 인덱싱"
echo "=========================================="
python index_csasi.py

if [ $? -ne 0 ]; then
    echo "❌ 인덱싱 실패!"
    exit 1
fi

echo ""
echo "=========================================="
echo "✅ 모든 작업 완료!"
echo "=========================================="
echo ""
echo "다음 명령어로 검색 테스트:"
echo '  curl -X POST "http://localhost:9200/csasi_consultation/_search?pretty" -H "Content-Type: application/json" -d'"'"'{"query":{"match":{"csasi_name":"환불"}}, "size":3}'"'"''
echo ""

