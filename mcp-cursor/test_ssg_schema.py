#!/usr/bin/env python3
"""SSG 스키마 테이블 조회 테스트"""
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

print("🔍 SSG 스키마 테이블 조회 테스트")
print()

try:
    # DSN 생성 및 연결
    dsn = oracledb.makedsn(
        ORACLE_CONFIG['host'],
        ORACLE_CONFIG['port'],
        service_name=ORACLE_CONFIG['service_name']
    )
    
    connection = oracledb.connect(
        user=ORACLE_CONFIG['user'],
        password=ORACLE_CONFIG['password'],
        dsn=dsn
    )
    
    print("✅ Oracle DB 연결 성공!")
    print()
    
    cursor = connection.cursor()
    
    # 1. 접근 가능한 스키마 목록
    print("=" * 80)
    print("📋 접근 가능한 스키마 목록")
    print("=" * 80)
    cursor.execute("""
        SELECT DISTINCT owner, COUNT(*) as table_count
        FROM all_tables
        GROUP BY owner
        ORDER BY owner
    """)
    
    schemas = cursor.fetchall()
    for schema in schemas:
        print(f"  {schema[0]}: {schema[1]}개 테이블")
    
    print()
    
    # 2. SSG 스키마의 테이블 목록 (최대 20개)
    print("=" * 80)
    print("📋 SSG 스키마 테이블 목록 (최대 20개)")
    print("=" * 80)
    cursor.execute("""
        SELECT table_name, num_rows, last_analyzed
        FROM all_tables
        WHERE owner = 'SSG'
        AND rownum <= 20
        ORDER BY table_name
    """)
    
    ssg_tables = cursor.fetchall()
    if ssg_tables:
        for table in ssg_tables:
            table_name = table[0]
            num_rows = table[1] if table[1] else "N/A"
            last_analyzed = table[2] if table[2] else "N/A"
            print(f"  - {table_name} ({num_rows} rows, analyzed: {last_analyzed})")
    else:
        print("  SSG 스키마에 접근 가능한 테이블이 없습니다.")
    
    print()
    
    # 3. SSG 스키마 총 테이블 수
    cursor.execute("""
        SELECT COUNT(*)
        FROM all_tables
        WHERE owner = 'SSG'
    """)
    total_count = cursor.fetchone()[0]
    print(f"✅ SSG 스키마 총 테이블 수: {total_count}개")
    
    cursor.close()
    connection.close()
    
    print("\n✅ 모든 테스트 완료!")
    
except Exception as e:
    print(f"❌ 오류 발생: {str(e)}")
    import traceback
    traceback.print_exc()

