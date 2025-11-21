#!/bin/bash
# Spring Boot + Python 임베딩 서버 완전 재시작 스크립트

echo "======================================"
echo "🔄 서버 재시작 스크립트"
echo "======================================"
echo ""

# ========================================
# 1. 기존 프로세스 종료
# ========================================
echo "🛑 기존 프로세스 종료 중..."

# Spring Boot 종료
pkill -9 -f "spring-boot-elasticsearch" 2>/dev/null
pkill -9 -f "gradlew bootRun" 2>/dev/null
echo "  ✓ Spring Boot 프로세스 종료"

# Python 서버 종료
pkill -9 -f "embedding_api.py" 2>/dev/null
pkill -9 -f "python.*5001" 2>/dev/null
echo "  ✓ Python 임베딩 서버 종료"

echo ""

# ========================================
# 2. Gradle Daemon 정리
# ========================================
echo "🧹 Gradle Daemon 정리 중..."
cd /Users/megasoby/Work/WorkSpace/Cursor/spring-boot-elasticsearch
./gradlew --stop 2>/dev/null
echo "  ✓ Gradle Daemon 정리 완료"

echo ""

# ========================================
# 3. 대기
# ========================================
echo "⏳ 3초 대기..."
sleep 3

echo ""

# ========================================
# 4. Python 임베딩 서버 시작
# ========================================
echo "🐍 Python 임베딩 서버 시작 중..."
cd /Users/megasoby/Work/WorkSpace/Cursor/spring-boot-elasticsearch/embedding

# 로그 파일 초기화
rm -f python_server.log

# 가상환경 활성화 및 Python 서버 백그라운드 실행
nohup ./venv/bin/python embedding_api.py > python_server.log 2>&1 &
PYTHON_PID=$!

echo "  ✓ Python 서버 시작됨 (PID: $PYTHON_PID)"
echo "  📝 로그: embedding/python_server.log"

echo ""

# ========================================
# 5. Spring Boot 시작
# ========================================
echo "🚀 Spring Boot 시작 중..."
cd /Users/megasoby/Work/WorkSpace/Cursor/spring-boot-elasticsearch

# 로그 파일 초기화
rm -f boot.log

# Spring Boot 백그라운드 실행
nohup ./gradlew bootRun > boot.log 2>&1 &
SPRING_PID=$!

echo "  ✓ Spring Boot 시작됨 (PID: $SPRING_PID)"
echo "  📝 로그: boot.log"

echo ""

# ========================================
# 6. 서버 부팅 대기
# ========================================
echo "⏳ 서버 부팅 대기 중..."
echo "  - Python 서버: 10초 대기"
sleep 10

# Python 서버 상태 확인
if curl -s http://localhost:5001/health > /dev/null 2>&1; then
    echo "  ✅ Python 서버 정상 실행 중 (http://localhost:5001)"
else
    echo "  ⚠️  Python 서버 응답 없음 (로그 확인 필요)"
fi

echo ""
echo "  - Spring Boot: 15초 대기"
sleep 15

# Spring Boot 상태 확인
if curl -s http://localhost:8081/ > /dev/null 2>&1; then
    echo "  ✅ Spring Boot 정상 실행 중 (http://localhost:8081)"
else
    echo "  ⚠️  Spring Boot 응답 없음 (로그 확인 필요)"
fi

echo ""

# ========================================
# 7. 로그 확인
# ========================================
echo "======================================"
echo "📋 로그 확인"
echo "======================================"

echo ""
echo "🐍 Python 서버 로그 (마지막 10줄):"
echo "--------------------------------------"
tail -10 /Users/megasoby/Work/WorkSpace/Cursor/spring-boot-elasticsearch/embedding/python_server.log 2>/dev/null || echo "  (로그 없음)"

echo ""
echo "🚀 Spring Boot 로그 (마지막 10줄):"
echo "--------------------------------------"
tail -10 /Users/megasoby/Work/WorkSpace/Cursor/spring-boot-elasticsearch/boot.log 2>/dev/null || echo "  (로그 없음)"

echo ""

# ========================================
# 8. 최종 상태
# ========================================
echo "======================================"
echo "✅ 재시작 완료!"
echo "======================================"
echo ""
echo "🌐 서비스 URL:"
echo "  - Spring Boot:    http://localhost:8081"
echo "  - Python 서버:    http://localhost:5001"
echo "  - 웹 UI:          http://localhost:8081/index.html"
echo ""
echo "📝 로그 확인 명령어:"
echo "  - Spring Boot:    tail -f boot.log"
echo "  - Python 서버:    tail -f embedding/python_server.log"
echo ""
echo "🔍 프로세스 확인:"
echo "  - Spring Boot:    lsof -i :8081"
echo "  - Python 서버:    lsof -i :5001"
echo ""

