# Spring Boot Elasticsearch + Kibana Project

Spring Boot와 Elasticsearch를 연동한 검색 기능이 포함된 프로젝트입니다.

## 🚀 시작하기

### 1. Docker로 Elasticsearch & Kibana 실행

```bash
docker-compose up -d
```

### 2. Gradle로 프로젝트 빌드 및 실행

```bash
./gradlew bootRun
```

### 3. 접속

- **Spring Boot API**: http://localhost:8080
- **Kibana**: http://localhost:5601
- **Elasticsearch**: http://localhost:9200

---

## 📚 API 엔드포인트

### 상품 관련 API

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/products` | 상품 생성 |
| GET | `/api/products` | 모든 상품 조회 |
| GET | `/api/products/{id}` | 특정 상품 조회 |
| PUT | `/api/products/{id}` | 상품 수정 |
| DELETE | `/api/products/{id}` | 상품 삭제 |
| GET | `/api/products/search/name?name=검색어` | 상품명으로 검색 |
| GET | `/api/products/search/category?category=카테고리` | 카테고리로 검색 |
| GET | `/api/products/search/price?minPrice=최소&maxPrice=최대` | 가격 범위로 검색 |

---

## 📝 사용 예시

### 상품 생성

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "노트북",
    "description": "고성능 노트북",
    "price": 1500000,
    "category": "전자제품",
    "stock": 10
  }'
```

### 상품 검색

```bash
curl http://localhost:8080/api/products/search/name?name=노트북
```

### 가격 범위 검색

```bash
curl "http://localhost:8080/api/products/search/price?minPrice=1000000&maxPrice=2000000"
```

---

## 🛠️ 기술 스택

- Spring Boot 3.2.0
- Elasticsearch 8.5.0
- Kibana 8.5.0
- Java 21
- Gradle 8.5

---

## 📦 의존성

- spring-boot-starter-web
- spring-boot-starter-data-elasticsearch
- elasticsearch-rest-client

---

## 🧹 정리

Docker 컨테이너 중지:

```bash
docker-compose down
```

---

## 💡 참고사항

- 기본 포트: Spring Boot (8080), Elasticsearch (9200), Kibana (5601)
- Elasticsearch는 단일 노드로 구성됨
- H2 데이터베이스는 포함되지 않음 (Elasticsearch 사용)

