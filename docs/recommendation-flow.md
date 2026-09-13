# 추천 결과 보존과 코스 지도

## API 흐름

1. `POST /api/recommendations` 응답의 `recommendationSessionId`를 보관합니다.
2. 주변 추천과 코스 생성에 동일한 ID 및 해당 응답의 메인 관광지 ID를 전달합니다.
3. 주변 장소를 선택할 경우 `/api/recommendations/nearby`를 먼저 호출합니다.
4. `/api/courses`에는 주변 장소 ID를 0~4개 전달합니다.

세션은 Firestore `recommendationSessions` 컬렉션에 JSON 스냅샷으로 저장되며,
생성 후 24시간 동안 사용할 수 있습니다. 만료 또는 존재하지 않는 세션은
400 응답을 반환하므로 추천부터 다시 시작합니다. 기존 `test-session-001`은
사용하지 않습니다. 현재 만료 세션 문서의 자동 삭제는 설정되어 있지 않습니다.

확정 지역과 여행 조건, 메인 관광지 한줄평 및 주변 추천 한줄평은 세션 값을
사용합니다. 클라이언트의 `region`은 생략하거나 `없음`이어도 됩니다.
코스 생성 시 관광공사/Groq를 재호출하지 않습니다. Gemini가 누락한 선택 장소는
생성된 순서 뒤에 사용자 선택 순서로 추가하며, 새로운 장소나 중복은 제외합니다.

## 카카오 지도 이미지

IntelliJ 실행 환경 변수에 `KAKAO_JAVASCRIPT_KEY`를 설정합니다.
REST API 키가 아닌 카카오 지도용 JavaScript 키를 사용합니다.
카카오 앱의 웹 도메인에 `http://localhost:8080`을 등록합니다.

서버의 Playwright가 카카오 Web 지도를 렌더링한 뒤 주황색 핀을 포함한
630×346 PNG를 캡처합니다. 카카오 로고·저작권 표시는 그대로 유지합니다.
이미지는 `mapImageUrl`에 `data:image/png;base64,...` 형태로 반환·저장됩니다.
프론트의 기존 결과 카드 배경과 카드 캡처 기능에서 바로 사용할 수 있습니다.
지도 로딩/캡처가 실패하면 빈 이미지를 저장하지 않고 502를 반환합니다.

설정:

- `course-map.origin`: 기본 `http://localhost:8080`. 등록된 웹 도메인과 일치해야 합니다.
- `course-map.browser-channel`: 기본 `msedge`. 이 PC의 설치된 Edge를 사용합니다.
  배포 서버에는 선택한 브라우저와 시스템 의존성을 별도로 설치해야 합니다.
- 환경 변수를 추가하거나 Gradle 의존성을 변경한 뒤에는 IntelliJ에서 Gradle을
  새로고침하고 서버를 완전히 종료한 후 다시 실행합니다.

이미지 크기는 data URL 기준 700,000자로 제한합니다. Firestore 문서 크기와
목록 응답 크기를 고려한 초기 구현이며, 코스 수가 늘면 이미지 객체 저장소와
목록 페이지네이션으로 전환할 수 있습니다.

## 검증

`gradlew.bat test --tests '*RecommendationFlowTests'`

외부 호출 없이 지역 정규화, 세션 지역 우선 적용, 한줄평 보존, 선택 장소 누락
복원, 다른 세션의 메인 장소 거절, 세션 JSON 왕복 변환을 확인합니다.
실제 카카오 지도 렌더링은 유효한 JavaScript 키·등록 도메인·브라우저가 필요합니다.

참고: https://apis.map.kakao.com/web/documentation/
https://playwright.dev/java/docs/screenshots
