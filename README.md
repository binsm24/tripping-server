# TripPing Server

> 사용자 요구 기반 당일치기 최적화 확장형 관광 코스 추천 서비스  
> 2026 관광데이터 활용 공모전

## 서비스 소개

TripPing은 사용자의 여행 조건과 자연어 요구사항을 분석하여
개인화된 관광지와 당일치기 여행 코스를 추천하는 서비스입니다.

여행 유형, 연령, 동행자, 지역 등의 선택 조건과
사용자가 직접 입력한 자연어 요구사항을 함께 반영합니다.

이후 사용자가 선택한 핵심 관광지를 중심으로
주변 관광지·카페·음식점을 단계적으로 확장 추천하고,
최종적으로 방문 순서와 예상 소요 시간이 포함된 여행 코스를 생성합니다.

## 주요 기능

| 기능 | 설명 |
|---|---|
| 카카오 로그인 | 카카오 Access Token을 이용한 로그인 및 회원가입 |
| 조건 기반 관광지 추천 | 여행 유형, 연령, 동행자, 지역을 기반으로 메인 관광지 추천 |
| 자연어 기반 추천 | 사용자가 입력한 추가 요구사항을 AI 추천에 반영 |
| 관광지 상세 조회 | 관광지 설명, 주소, 이미지, 위치 정보 조회 |
| 주변 장소 확장 추천 | 선택한 관광지 주변의 명소·카페·음식점 추천 |
| AI 여행 코스 생성 | 코스명, 태그, 소개, 예상 소요 시간, 방문 순서 생성 |
| 코스 지도 생성 | 선택한 장소를 기반으로 지도 이미지 생성 |
| 코스 저장 | 생성된 여행 코스를 보관함에 저장 |
| 보관함 조회 | 저장된 여행 코스 목록 및 상세 정보 조회 |

## 서비스 흐름

```text
여행 조건 및 자연어 요구사항 입력
                ↓
       AI 메인 관광지 3곳 추천
                ↓
          사용자가 관광지 선택
                ↓
 주변 관광지·카페·음식점 확장 추천
                ↓
          사용자가 장소 선택
                ↓
           AI 여행 코스 생성
                ↓
     코스 카드 및 지도 이미지 제공
                ↓
          코스 저장 및 보관함 조회
```

## 기술 스택

### Backend

- Java 21
- Spring Boot 4.0.7
- Spring MVC
- Gradle
- Lombok
- Jakarta Bean Validation

### API Documentation

- Springdoc OpenAPI
- Swagger UI

### 연동 예정 외부 서비스

- Google Gemini API
- 한국관광공사 관광정보 OpenAPI
- Kakao 지도 API
- Kakao 로그인 API
- Kakao Static Map API
- Firebase

## 프로젝트 구조

```text
src/main/java/com/tripping/trippingserver
├── TrippingServerApplication.java
├── config
├── controller
├── dto
│   ├── request
│   └── response
├── entity
├── exception
├── external (추가)
│   └── tourism
├── repository
└── service
```

| 패키지 | 역할 |
|---|---|
| `controller` | HTTP 요청 및 응답 처리 |
| `service` | 비즈니스 로직 처리 |
| `dto.request` | 클라이언트 요청 데이터 |
| `dto.response` | 서버 응답 데이터 |
| `entity` | 데이터베이스 저장 객체 |
| `repository` | 데이터 저장소 접근 |
| `config` | Swagger 및 외부 서비스 설정 |
| `exception` | 예외 및 공통 오류 처리 |
| `external.tourism` | 한국관광공사 OpenAPI 호출 및 응답 변환 |

## API 목록

### 인증

| 기능 | Method | Endpoint |
|---|---:|---|
| 카카오 로그인 | `POST` | `/api/auth/kakao` |

### 관광지

| 기능 | Method | Endpoint |
|---|---:|---|
| AI 메인 관광지 추천 | `POST` | `/api/recommendations` |
| 주변 장소 확장 추천 | `POST` | `/api/recommendations/nearby` |
| 관광지 상세 조회 | `GET` | `/api/places/{placeId}` |

### 여행 코스

| 기능 | Method | Endpoint |
|---|---:|---|
| AI 여행 코스 생성 | `POST` | `/api/courses` |
| 코스 저장 | `POST` | `/api/saved-courses` |
| 보관함 목록 조회 | `GET` | `/api/saved-courses` |
| 보관함 상세 조회 | `GET` | `/api/saved-courses/{savedCourseId}` |

## 공통 응답 형식

TripPing API는 다음과 같은 공통 응답 구조를 사용합니다.

```json
{
  "status": 200,
  "success": true,
  "message": "course created successfully",
  "data": {}
}
```

| 필드 | 설명 |
|---|---|
| `status` | HTTP 상태 코드 |
| `success` | 요청 성공 여부 |
| `message` | 처리 결과 메시지 |
| `data` | 실제 응답 데이터 |

## 한국관광공사 OpenAPI 연동

TripPing은 한국관광공사 국문 관광정보 OpenAPI를 연동하여
관광지 상세 정보와 위치 기반 주변 장소 정보를 실데이터로 조회합니다.

### 사용 API

- `detailCommon2`
  - 관광지 상세 정보 조회
- `locationBasedList2`
  - 위도·경도를 기준으로 주변 장소 조회

관광공사 API Key는 코드에 직접 작성하지 않고 환경변수로 관리합니다.

```properties
tourism.api-key=${TOURISM_API_KEY:}
tourism.base-url=${TOURISM_BASE_URL:https://apis.data.go.kr/B551011/KorService2}
```

관광공사의 `contentId`는 TripPing 내부에서 다음과 같은 형식의 `placeId`로 사용합니다.

```text
tourism-133854
```

예시:

```text
관광공사 contentId : 133854
TripPing placeId   : tourism-133854
```

### 관광지 상세 조회

```http
GET /api/places/{placeId}
```

요청 예시:

```http
GET /api/places/tourism-133854
```

한국관광공사의 관광지 상세 정보를 조회한 뒤
TripPing에서 사용하는 응답 DTO 형태로 변환하여 반환합니다.

주요 응답 정보:

- 관광지 ID
- 관광지명
- 설명
- 이미지 URL
- 주소
- 전화번호
- 위도
- 경도
- 지도 URL

존재하지 않는 관광지를 조회할 경우
`404 PLACE_NOT_FOUND`를 반환합니다.

관광공사 API 호출 중 오류가 발생할 경우
`502 EXTERNAL_API_ERROR`를 반환합니다.

### 주변 장소 확장 추천

```http
POST /api/recommendations/nearby
Content-Type: application/json
```

요청 예시:

```json
{
  "mainPlaceId": "tourism-133854",
  "recommendationSessionId": "test-session-001"
}
```

선택한 메인 관광지의 위도·경도를 기준으로
반경 3km 이내의 주변 장소를 한국관광공사 API에서 조회합니다.

현재 주변 장소는 관광공사의 `contentTypeId`를 기준으로 분류합니다.

- `39` : 음식점
- 그 외 : 주변 관광지
- 카페 : 추후 Kakao Local API 연동 단계에서 보완 예정

관광지와 음식점은 각각 최대 10개까지 반환합니다.

### 관광공사 연동 구조

```text
Controller
    ↓
Service
    ↓
TourismApiClient
    ↓
한국관광공사 OpenAPI
    ↓
TourismApiResponse
    ↓
TourismPlaceMapper
    ↓
TripPing Response DTO
```

관광공사 관련 코드는 다음 패키지에서 관리합니다.

```text
external/tourism
├── TourismApiClient
├── TourismApiProperties
├── TourismApiResponse
└── TourismPlaceMapper
```

### 4·5단계 구현 내용

- 한국관광공사 관광정보 OpenAPI 연동
- 관광지 상세 조회 실데이터 적용
- 위치 기반 주변 장소 조회 구현
- 관광공사 응답 DTO 및 Mapper 구현
- 관광지 위도·경도 실데이터 적용
- 주변 관광지 및 음식점 분류
- 관광지·음식점 각각 최대 10개 반환
- 존재하지 않는 관광지 `404 PLACE_NOT_FOUND` 처리
- 외부 API 오류 `502 EXTERNAL_API_ERROR` 처리
- Swagger API 문서 및 응답 코드 정리

> `POST /api/recommendations`의 AI 메인 관광지 추천 기능은 Gemini API 연동 단계에서 실제 데이터로 전환할 예정이며, 현재는 기존 임시 응답을 유지합니다.

## 주요 요청 예시

### 메인 관광지 추천

```http
POST /api/recommendations
Content-Type: application/json
```

```json
{
  "travelType": "자연",
  "age": 20,
  "companion": "친구",
  "region": "경기도 수원",
  "requirement": "조용한 카페가 많고 산책하기 좋은 곳"
}
```

`requirement`는 선택 입력값이므로 생략할 수 있습니다.

```json
{
  "travelType": "자연",
  "age": 20,
  "companion": "친구",
  "region": "경기도 수원"
}
```

### 관광지 상세 조회

```http
GET /api/places/tourism-133854
```

정상적인 관광지 ID를 요청하면
한국관광공사 OpenAPI에서 조회한 관광지 상세 정보를 반환합니다.

### 주변 장소 확장 추천

```http
POST /api/recommendations/nearby
Content-Type: application/json
```

```json
{
  "mainPlaceId": "tourism-133854",
  "recommendationSessionId": "test-session-001"
}
```

### AI 여행 코스 생성

```http
POST /api/courses
Content-Type: application/json
```

```json
{
  "mainPlaceId": "place-001",
  "selectedPlaceIds": [
    "place-001",
    "nearby-cafe-001",
    "nearby-restaurant-001"
  ]
}
```

### 코스 저장

```http
POST /api/saved-courses
Content-Type: application/json
```

```json
{
  "courseId": 1
}
```

## Swagger 실행

Spring Boot 애플리케이션을 실행한 뒤 아래 주소에서 Swagger UI를 확인할 수 있습니다.

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON 문서:

```text
http://localhost:8080/v3/api-docs
```

## 로컬 실행 방법

Windows:

```bash
gradlew.bat bootRun
```

macOS / Linux:

```bash
./gradlew bootRun
```

또는 IntelliJ에서 다음 클래스를 실행합니다.

```text
com.tripping.trippingserver.TrippingServerApplication
```

## 환경 변수 및 보안

API Key와 인증 정보는 GitHub에 직접 저장하지 않습니다.

예시:

```properties
gemini.api.key=${GEMINI_API_KEY:}
kakao.rest-api-key=${KAKAO_REST_API_KEY:}
tourism.api-key=${TOURISM_API_KEY:}
```

다음 파일은 Repository에 커밋하지 않습니다.

```text
.env
application-local.properties
application-secret.properties
firebase-service-account.json
*-firebase-adminsdk-*.json
```

## 현재 개발 상태

### 완료

- Spring Boot 4.0.7 프로젝트 구성
- Java 21 환경 구성
- Swagger / OpenAPI 연동
- 공통 응답 및 예외 처리 구조 구현
- Firebase Admin 초기화
- Firestore 코스 저장 및 보관함 조회 연동
- 한국관광공사 관광정보 OpenAPI 연동
- 관광공사 상세조회 API 연동
- 관광공사 위치 기반 주변 장소 조회 API 연동
- 관광공사 응답 DTO 및 Mapper 구현
- `GET /api/places/{placeId}` 실데이터 적용
- `POST /api/recommendations/nearby` 실데이터 적용
- 관광지 위도·경도 실데이터 적용
- 주변 장소 관광지/음식점 분류
- 주변 장소 종류별 최대 10개 반환
- 존재하지 않는 관광지 조회 시 `404 PLACE_NOT_FOUND` 처리
- 관광공사 API 오류 시 `502 EXTERNAL_API_ERROR` 처리
- 관광공사 API Key 환경변수 관리

### 진행 예정

- Gemini API 실제 연동
- AI 메인 관광지 추천 실데이터 적용
- Kakao Local API 연동
- 카페 데이터 보완
- Kakao 지도 및 Static Map API 연동
- Kakao 로그인 실제 연동
- 인증 토큰 적용
- 프론트엔드 연결
- 테스트 코드 작성
- 배포 환경 구성

> `POST /api/recommendations`는 Gemini 연동 단계 전까지 API 구조 검증을 위한 임시 데이터를 반환합니다.

## 브랜치 전략

```text
main       : 배포 가능한 안정 버전
develop    : 개발 통합 브랜치
feature/*  : 기능 개발 브랜치
fix/*      : 버그 수정 브랜치
```

## 커밋 메시지 예시

```text
feat: add recommendation API
feat: add saved course API
fix: correct course response type
docs: update backend README
refactor: separate response DTO
chore: configure Swagger
test: add recommendation controller test
```

## 관련 Repository

- Frontend: (https://github.com/enhn16/TripPing-frontend)
- Backend: (https://github.com/binsm24/tripping-server)

## License

현재 라이선스는 팀 논의 후 결정합니다.
