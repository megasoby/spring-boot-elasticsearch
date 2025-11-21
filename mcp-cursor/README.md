# Cursor AI용 Oracle MCP 서버

웬즈데이(Cursor AI)가 Oracle DB에 직접 연결해서 데이터를 조회할 수 있게 해주는 MCP 서버입니다.

## 🎯 기능

### 1. `query_oracle`
- Oracle DB에 SELECT 쿼리 실행
- 안전을 위해 SELECT 문만 허용

### 2. `list_tables`
- 현재 사용자가 접근 가능한 모든 테이블 목록 조회
- 테이블별 행 수와 마지막 분석일 표시

### 3. `describe_table`
- 특정 테이블의 컬럼 정보 조회
- 컬럼명, 데이터타입, 길이, NULL 허용 여부, 기본값 표시

## 🔧 설정

### Oracle DB 연결 정보
`config.env` 파일에 저장되어 있습니다:

```env
ORACLE_HOST=10.203.7.71
ORACLE_PORT=1538
ORACLE_SERVICE=DEVUTFDB
ORACLE_USER=DEVSSG
ORACLE_PASSWORD=d2vssg12#
```

### Cursor 설정
Cursor 설정 파일에 다음을 추가하세요:

**위치**: 
- Mac: `~/Library/Application Support/Cursor/mcp.json`
- Windows: `%APPDATA%\Cursor\mcp.json`

```json
{
  "mcpServers": {
    "oracle-db": {
      "command": "/Users/megasoby/Work/WorkSpace/Cursor/spring-boot-elasticsearch/mcp-cursor/venv/bin/python",
      "args": [
        "/Users/megasoby/Work/WorkSpace/Cursor/spring-boot-elasticsearch/mcp-cursor/mcp_cursor_server.py"
      ],
      "env": {}
    }
  }
}
```

## 🚀 사용 방법

### Cursor에서 사용
Cursor를 재시작한 후, Composer나 Chat에서 다음과 같이 요청하세요:

```
웬즈데이, Oracle DB의 테이블 목록 보여줘
```

```
TBL_BOARD 테이블 구조 알려줘
```

```
TBL_BOARD에서 최근 10개 데이터 조회해줘
```

### 직접 테스트
```bash
cd /Users/megasoby/Work/WorkSpace/Cursor/spring-boot-elasticsearch/mcp-cursor
./venv/bin/python test_oracle.py
```

## 📊 현재 DB 상태

- **Oracle Version**: 19c Enterprise Edition
- **테이블 수**: 4개
  - KJS_20250101
  - PLAN_TABLE
  - TBL_BOARD
  - TEST_TABLE4

## 🔒 보안

- SELECT 쿼리만 실행 가능 (INSERT, UPDATE, DELETE 불가)
- 연결 정보는 `config.env`에 저장 (gitignore 처리 권장)
- 환경변수로 관리 가능

## 🛠️ 문제 해결

### 연결 실패 시
1. Oracle DB가 실행 중인지 확인
2. 네트워크 연결 확인
3. 연결 정보 확인 (`config.env`)
4. 테스트 스크립트 실행: `./venv/bin/python test_oracle.py`

### Cursor에서 도구가 안 보일 때
1. Cursor 완전히 종료
2. Cursor 재시작
3. 설정 파일 경로 확인

