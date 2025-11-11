#!/usr/bin/env python3
"""
MCP 서버: Claude Desktop과 Spring Boot RAG API를 연결
"""
import asyncio
import logging
from typing import Any
import requests
from mcp.server import Server
from mcp.server.stdio import stdio_server
from mcp.types import Tool, TextContent

# 로깅 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Spring Boot API URL
RAG_API_URL = "http://localhost:8081/api/rag/search"

# MCP 서버 생성
app = Server("product-search-server")

@app.list_tools()
async def list_tools() -> list[Tool]:
    """
    Claude가 사용할 수 있는 Tool 목록 정의
    """
    return [
        Tool(
            name="search_products",
            description="상품 검색 및 추천을 위한 RAG 검색 도구입니다. 사용자의 질문을 받아 유사한 상품을 검색하고, AI가 이해할 수 있는 컨텍스트를 반환합니다.",
            inputSchema={
                "type": "object",
                "properties": {
                    "query": {
                        "type": "string",
                        "description": "검색할 질문 또는 키워드 (예: '스마트워치 추천해줘', '30만원대 노트북')"
                    },
                    "top_k": {
                        "type": "integer",
                        "description": "검색할 상품 개수 (기본값: 5)",
                        "default": 5
                    }
                },
                "required": ["query"]
            }
        )
    ]

@app.call_tool()
async def call_tool(name: str, arguments: Any) -> list[TextContent]:
    """
    Claude가 Tool을 호출했을 때 실행되는 함수
    """
    if name != "search_products":
        raise ValueError(f"Unknown tool: {name}")
    
    query = arguments.get("query")
    top_k = arguments.get("top_k", 5)
    
    logger.info(f"🔍 RAG 검색 요청: query='{query}', top_k={top_k}")
    
    try:
        # Spring Boot RAG API 호출
        response = requests.post(
            RAG_API_URL,
            json={"query": query, "topK": top_k},
            timeout=30
        )
        response.raise_for_status()
        
        rag_result = response.json()
        context = rag_result.get("context", "")
        products = rag_result.get("products", [])
        
        logger.info(f"✅ RAG 검색 완료: {len(products)}개 상품 검색됨")
        
        # Claude에게 컨텍스트 반환
        return [
            TextContent(
                type="text",
                text=context
            )
        ]
        
    except requests.exceptions.RequestException as e:
        error_msg = f"❌ RAG API 호출 실패: {str(e)}"
        logger.error(error_msg)
        return [
            TextContent(
                type="text",
                text=f"오류가 발생했습니다: {error_msg}\n\nSpring Boot 서버(port 8081)와 Elasticsearch가 실행 중인지 확인해주세요."
            )
        ]

async def main():
    """MCP 서버 실행"""
    logger.info("🚀 MCP 서버 시작!")
    logger.info(f"📡 Spring Boot RAG API: {RAG_API_URL}")
    
    async with stdio_server() as (read_stream, write_stream):
        await app.run(
            read_stream,
            write_stream,
            app.create_initialization_options()
        )

if __name__ == "__main__":
    asyncio.run(main())

