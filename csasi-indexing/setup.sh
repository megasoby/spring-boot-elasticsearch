#!/bin/bash

echo "=========================================="
echo "🔧 CSASI 인덱싱 환경 설정"
echo "=========================================="

# 현재 디렉토리로 이동
cd "$(dirname "$0")"

# 1. Python 가상환경 생성
echo "📦 Python 가상환경 생성 중..."
python3 -m venv venv

# 2. 가상환경 활성화
echo "✅ 가상환경 활성화..."
source venv/bin/activate

# 3. 패키지 설치
echo "📥 패키지 설치 중..."
pip install --upgrade pip
pip install -r requirements.txt

echo ""
echo "=========================================="
echo "✅ 환경 설정 완료!"
echo "=========================================="
echo ""
echo "다음 단계:"
echo "  1. Elasticsearch 실행 확인"
echo "  2. Python Embedding API 실행 확인 (port 5001)"
echo "  3. ./run_indexing.sh 실행"
echo ""

