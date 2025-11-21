#!/usr/bin/env python3
"""Oracle DB 연결 테스트"""
import oracledb
from dotenv import load_dotenv
import os

# 환경변수 로드
load_dotenv('config.env')

ORACLE_CONFIG = {
    'host': os.getenv('ORACLE_HOST', '10.203.7.71'),
    'port': int(os.getenv('ORACLE_PORT', '1538')),
    'service_name': os.getenv('ORACLE_SERVICE', 'DEVUTFDB'),
    'user': os.getenv('ORACLE_USER', 'DEVSSG'),
    'password': os.getenv('ORACLE_PASSWORD', 'd2vssg12#')
}

print("🔍 Oracle DB 연결 테스트")
print(f"📡 Host: {ORACLE_CONFIG['host']}:{ORACLE_CONFIG['port']}")
print(f"📡 Service: {ORACLE_CONFIG['service_name']}")
print(f"👤 User: {ORACLE_CONFIG['user']}")
print()

try:
    # DSN 생성
    dsn = oracledb.makedsn(
        ORACLE_CONFIG['host'],
        ORACLE_CONFIG['port'],
        service_name=ORACLE_CONFIG['service_name']
    )
    print(f"✅ DSN 생성 완료: {dsn}")
    
    # 연결 시도
    print("🔗 연결 시도 중...")
    connection = oracledb.connect(
        user=ORACLE_CONFIG['user'],
        password=ORACLE_CONFIG['password'],
        dsn=dsn
    )
    
    print("✅ Oracle DB 연결 성공!")
    
    # 버전 확인
    cursor = connection.cursor()
    cursor.execute("SELECT * FROM v$version WHERE banner LIKE 'Oracle%'")
    version = cursor.fetchone()
    print(f"📌 Oracle Version: {version[0]}")
    
    # 테이블 개수 확인
    cursor.execute("SELECT COUNT(*) FROM user_tables")
    table_count = cursor.fetchone()[0]
    print(f"📊 사용 가능한 테이블 수: {table_count}개")
    
    # 테이블 목록 (최대 10개)
    cursor.execute("""
        SELECT table_name, num_rows 
        FROM user_tables 
        WHERE rownum <= 10
        ORDER BY table_name
    """)
    
    print("\n📋 테이블 목록 (최대 10개):")
    for row in cursor.fetchall():
        print(f"  - {row[0]} ({row[1] if row[1] else 'N/A'} rows)")
    
    cursor.close()
    connection.close()
    
    print("\n✅ 모든 테스트 완료!")
    
except Exception as e:
    print(f"❌ 오류 발생: {str(e)}")
    import traceback
    traceback.print_exc()

