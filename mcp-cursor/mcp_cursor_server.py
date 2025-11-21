#!/usr/bin/env python3
"""
MCP 서버: Cursor AI와 Oracle DB를 연결
웬즈데이가 Oracle DB의 상담 데이터를 직접 조회할 수 있게 해줍니다.
"""
import asyncio
import logging
import os
from typing import Any, Optional
import oracledb
from mcp.server import Server
from mcp.server.stdio import stdio_server
from mcp.types import Tool, TextContent
from dotenv import load_dotenv

# 환경변수 로드
load_dotenv('config.env')

# 로깅 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Oracle DB 연결 정보
ORACLE_CONFIG = {
    'host': os.getenv('ORACLE_HOST', '10.203.7.71'),
    'port': int(os.getenv('ORACLE_PORT', '1538')),
    'service_name': os.getenv('ORACLE_SERVICE', 'DEVUTFDB'),
    'user': os.getenv('ORACLE_USER', 'DEVSSG'),
    'password': os.getenv('ORACLE_PASSWORD', 'd2vssg12#')
}

# MCP 서버 생성
app = Server("oracle-db-server")

def get_oracle_connection():
    """Oracle DB 연결 생성"""
    try:
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
        return connection
    except Exception as e:
        logger.error(f"Oracle 연결 실패: {str(e)}")
        raise

@app.list_tools()
async def list_tools() -> list[Tool]:
    """
    Cursor AI(웬즈데이)가 사용할 수 있는 Tool 목록
    """
    return [
        Tool(
            name="query_oracle",
            description="Oracle DB에 SQL 쿼리를 실행합니다. SELECT 문만 실행 가능합니다.",
            inputSchema={
                "type": "object",
                "properties": {
                    "sql": {
                        "type": "string",
                        "description": "실행할 SQL 쿼리 (SELECT 문만 가능)"
                    },
                    "limit": {
                        "type": "integer",
                        "description": "조회할 최대 행 수 (기본값: 100)",
                        "default": 100
                    }
                },
                "required": ["sql"]
            }
        ),
        Tool(
            name="list_tables",
            description="특정 스키마의 테이블 목록을 조회합니다. 스키마를 지정하지 않으면 SSG 스키마를 조회합니다.",
            inputSchema={
                "type": "object",
                "properties": {
                    "schema": {
                        "type": "string",
                        "description": "조회할 스키마명 (기본값: SSG)",
                        "default": "SSG"
                    }
                },
                "required": []
            }
        ),
        Tool(
            name="describe_table",
            description="특정 테이블의 컬럼 정보를 조회합니다. 스키마명.테이블명 형식으로 지정 가능합니다.",
            inputSchema={
                "type": "object",
                "properties": {
                    "table_name": {
                        "type": "string",
                        "description": "조회할 테이블명 (예: SSG.테이블명 또는 테이블명)"
                    },
                    "schema": {
                        "type": "string",
                        "description": "스키마명 (기본값: SSG)",
                        "default": "SSG"
                    }
                },
                "required": ["table_name"]
            }
        ),
        Tool(
            name="list_schemas",
            description="접근 가능한 모든 스키마(사용자) 목록을 조회합니다.",
            inputSchema={
                "type": "object",
                "properties": {},
                "required": []
            }
        )
    ]

@app.call_tool()
async def call_tool(name: str, arguments: Any) -> list[TextContent]:
    """
    Cursor AI(웬즈데이)가 Tool을 호출했을 때 실행되는 함수
    """
    try:
        if name == "query_oracle":
            return await execute_query(arguments)
        elif name == "list_tables":
            return await list_oracle_tables(arguments)
        elif name == "describe_table":
            return await describe_oracle_table(arguments)
        elif name == "list_schemas":
            return await list_oracle_schemas()
        else:
            raise ValueError(f"Unknown tool: {name}")
    except Exception as e:
        error_msg = f"❌ 오류 발생: {str(e)}"
        logger.error(error_msg)
        return [TextContent(type="text", text=error_msg)]

async def execute_query(arguments: Any) -> list[TextContent]:
    """SQL 쿼리 실행"""
    sql = arguments.get("sql", "").strip()
    limit = arguments.get("limit", 100)
    
    # SELECT 문만 허용
    if not sql.upper().startswith("SELECT"):
        return [TextContent(
            type="text",
            text="❌ SELECT 문만 실행 가능합니다."
        )]
    
    logger.info(f"🔍 SQL 실행: {sql[:100]}...")
    
    try:
        connection = get_oracle_connection()
        cursor = connection.cursor()
        
        # 쿼리 실행
        cursor.execute(sql)
        
        # 컬럼명 가져오기
        columns = [desc[0] for desc in cursor.description]
        
        # 결과 가져오기 (limit 적용)
        rows = cursor.fetchmany(limit)
        
        cursor.close()
        connection.close()
        
        # 결과 포맷팅
        if not rows:
            result = "조회 결과가 없습니다."
        else:
            result = f"📊 조회 결과: {len(rows)}개 행\n\n"
            result += "컬럼: " + " | ".join(columns) + "\n"
            result += "-" * 80 + "\n"
            
            for row in rows:
                row_str = " | ".join(str(val) if val is not None else "NULL" for val in row)
                result += row_str + "\n"
        
        logger.info(f"✅ 쿼리 실행 완료: {len(rows)}개 행 조회")
        
        return [TextContent(type="text", text=result)]
        
    except Exception as e:
        error_msg = f"❌ 쿼리 실행 실패: {str(e)}"
        logger.error(error_msg)
        return [TextContent(type="text", text=error_msg)]

async def list_oracle_tables(arguments: Any) -> list[TextContent]:
    """테이블 목록 조회"""
    schema = arguments.get("schema", "SSG").upper()
    
    logger.info(f"📋 테이블 목록 조회 중... (스키마: {schema})")
    
    try:
        connection = get_oracle_connection()
        cursor = connection.cursor()
        
        # 특정 스키마의 테이블 목록 조회
        cursor.execute("""
            SELECT table_name, num_rows, last_analyzed
            FROM all_tables
            WHERE owner = :schema
            ORDER BY table_name
        """, {"schema": schema})
        
        rows = cursor.fetchall()
        
        cursor.close()
        connection.close()
        
        if not rows:
            result = f"스키마 '{schema}'에서 조회 가능한 테이블이 없습니다."
        else:
            result = f"📋 스키마 '{schema}' 테이블 목록 ({len(rows)}개)\n\n"
            result += "테이블명 | 행 수 | 마지막 분석일\n"
            result += "-" * 80 + "\n"
            
            for row in rows:
                table_name = row[0]
                num_rows = row[1] if row[1] is not None else "N/A"
                last_analyzed = row[2] if row[2] is not None else "N/A"
                result += f"{table_name} | {num_rows} | {last_analyzed}\n"
        
        logger.info(f"✅ 테이블 목록 조회 완료: {len(rows)}개")
        
        return [TextContent(type="text", text=result)]
        
    except Exception as e:
        error_msg = f"❌ 테이블 목록 조회 실패: {str(e)}"
        logger.error(error_msg)
        return [TextContent(type="text", text=error_msg)]

async def describe_oracle_table(arguments: Any) -> list[TextContent]:
    """테이블 구조 조회"""
    table_name = arguments.get("table_name", "").strip().upper()
    schema = arguments.get("schema", "SSG").upper()
    
    # 테이블명에 스키마가 포함되어 있으면 분리
    if "." in table_name:
        parts = table_name.split(".")
        schema = parts[0].upper()
        table_name = parts[1].upper()
    
    logger.info(f"📝 테이블 구조 조회: {schema}.{table_name}")
    
    try:
        connection = get_oracle_connection()
        cursor = connection.cursor()
        
        # 테이블 컬럼 정보 조회
        cursor.execute("""
            SELECT column_name, data_type, data_length, nullable, data_default
            FROM all_tab_columns
            WHERE owner = :schema AND table_name = :table_name
            ORDER BY column_id
        """, {"schema": schema, "table_name": table_name})
        
        rows = cursor.fetchall()
        
        cursor.close()
        connection.close()
        
        if not rows:
            result = f"❌ 테이블 '{schema}.{table_name}'을 찾을 수 없습니다."
        else:
            result = f"📝 테이블: {schema}.{table_name} ({len(rows)}개 컬럼)\n\n"
            result += "컬럼명 | 데이터타입 | 길이 | NULL허용 | 기본값\n"
            result += "-" * 100 + "\n"
            
            for row in rows:
                col_name = row[0]
                data_type = row[1]
                data_length = row[2] if row[2] is not None else ""
                nullable = "Y" if row[3] == "Y" else "N"
                default_val = row[4] if row[4] is not None else ""
                
                result += f"{col_name} | {data_type} | {data_length} | {nullable} | {default_val}\n"
        
        logger.info(f"✅ 테이블 구조 조회 완료: {len(rows)}개 컬럼")
        
        return [TextContent(type="text", text=result)]
        
    except Exception as e:
        error_msg = f"❌ 테이블 구조 조회 실패: {str(e)}"
        logger.error(error_msg)
        return [TextContent(type="text", text=error_msg)]

async def list_oracle_schemas() -> list[TextContent]:
    """스키마 목록 조회"""
    logger.info("📋 스키마 목록 조회 중...")
    
    try:
        connection = get_oracle_connection()
        cursor = connection.cursor()
        
        # 접근 가능한 스키마 목록 조회
        cursor.execute("""
            SELECT DISTINCT owner, COUNT(*) as table_count
            FROM all_tables
            GROUP BY owner
            ORDER BY owner
        """)
        
        rows = cursor.fetchall()
        
        cursor.close()
        connection.close()
        
        if not rows:
            result = "조회 가능한 스키마가 없습니다."
        else:
            result = f"📋 접근 가능한 스키마 목록 ({len(rows)}개)\n\n"
            result += "스키마명 | 테이블 수\n"
            result += "-" * 50 + "\n"
            
            for row in rows:
                schema_name = row[0]
                table_count = row[1]
                result += f"{schema_name} | {table_count}개\n"
        
        logger.info(f"✅ 스키마 목록 조회 완료: {len(rows)}개")
        
        return [TextContent(type="text", text=result)]
        
    except Exception as e:
        error_msg = f"❌ 스키마 목록 조회 실패: {str(e)}"
        logger.error(error_msg)
        return [TextContent(type="text", text=error_msg)]

async def main():
    """MCP 서버 실행"""
    logger.info("🚀 Cursor AI용 Oracle MCP 서버 시작!")
    logger.info(f"📡 Oracle DB: {ORACLE_CONFIG['host']}:{ORACLE_CONFIG['port']}/{ORACLE_CONFIG['service_name']}")
    logger.info(f"👤 User: {ORACLE_CONFIG['user']}")
    
    # 연결 테스트
    try:
        connection = get_oracle_connection()
        logger.info("✅ Oracle DB 연결 성공!")
        connection.close()
    except Exception as e:
        logger.error(f"❌ Oracle DB 연결 실패: {str(e)}")
        logger.error("서버를 시작하지만 DB 연결에 문제가 있을 수 있습니다.")
    
    async with stdio_server() as (read_stream, write_stream):
        await app.run(
            read_stream,
            write_stream,
            app.create_initialization_options()
        )

if __name__ == "__main__":
    asyncio.run(main())

