package com.tripping.trippingserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripping.trippingserver.dto.gemini.RegionSelectionResponse;
import com.tripping.trippingserver.dto.request.NearbyRecommendationRequest;
import com.tripping.trippingserver.dto.request.RecommendationRequest;
import com.tripping.trippingserver.dto.response.NearbyRecommendationResponse;
import com.tripping.trippingserver.dto.response.PlaceDetailResponse;
import com.tripping.trippingserver.dto.response.RecommendationResponse;
import com.tripping.trippingserver.exception.BusinessException;
import com.tripping.trippingserver.exception.ErrorCode;
import com.tripping.trippingserver.external.gemini.GeminiApiClient;
import com.tripping.trippingserver.external.tourism.TourismApiClient;
import com.tripping.trippingserver.external.tourism.TourismApiResponse;
import com.tripping.trippingserver.external.tourism.TourismPlaceMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.tripping.trippingserver.dto.gemini.GeminiCourseResponse;
import com.tripping.trippingserver.dto.request.CourseGenerationRequest;
import com.tripping.trippingserver.dto.gemini.GeminiPlaceRecommendationResponse;
import com.tripping.trippingserver.dto.response.CoursePlaceResponse;
import com.tripping.trippingserver.dto.response.CourseResponse;

@Service
public class RecommendationService {

    private final TourismApiClient tourismApiClient;
    private final TourismPlaceMapper tourismPlaceMapper;
    private final GeminiApiClient geminiApiClient;
    private final PredefinedPlaceCandidateService candidateService;
    private final ObjectMapper objectMapper;
    private static final int NEARBY_RADIUS_METERS = 5000;

    public RecommendationService(
            TourismApiClient tourismApiClient,
            TourismPlaceMapper tourismPlaceMapper,
            GeminiApiClient geminiApiClient,
            PredefinedPlaceCandidateService candidateService,
            ObjectMapper objectMapper
    ) {
        this.tourismApiClient = tourismApiClient;
        this.tourismPlaceMapper = tourismPlaceMapper;
        this.geminiApiClient = geminiApiClient;
        this.candidateService = candidateService;
        this.objectMapper = objectMapper;
    }

    public RecommendationResponse recommend(
            RecommendationRequest request
    ) {
        String selectedRegion = resolveRegion(request);

        TourismApiResponse tourismResponse =
                tourismApiClient.searchPlacesByKeyword(
                        selectedRegion
                );

        List<TourismApiResponse.Item> candidateItems =
                getValidCandidateItems(tourismResponse);

        if (candidateItems.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.PLACE_NOT_FOUND,
                    "선택한 지역에서 관광지를 찾을 수 없습니다."
            );
        }

        List<RecommendationResponse.RecommendedPlace> places =
                recommendPlacesByGemini(
                        request,
                        selectedRegion,
                        candidateItems
                );

        return RecommendationResponse.builder()
                .selectedRegion(selectedRegion)
                .title(selectedRegion + " 맞춤 여행 추천")
                .recommendationSessionId("test-session-001")
                .places(places)
                .build();
    }

    private List<TourismApiResponse.Item> getValidCandidateItems(
            TourismApiResponse response
    ) {
        if (response == null
                || response.getResponse() == null
                || response.getResponse().getBody() == null
                || response.getResponse()
                .getBody()
                .getItems() == null
                || response.getResponse()
                .getBody()
                .getItems()
                .getItem() == null) {
            return List.of();
        }

        return response.getResponse()
                .getBody()
                .getItems()
                .getItem()
                .stream()
                .filter(this::isGyeonggiPlace)
                .filter(this::hasValidCoordinates)
                .filter(item ->
                        item.getContentid() != null
                                && !item.getContentid().isBlank()
                )
                .limit(30)
                .toList();
    }

    private String resolveRegion(
            RecommendationRequest request
    ) {
        String region = request.getRegion();

        // region 미입력: Gemini가 후보 중 하나 선택
        if (region == null || region.isBlank()) {
            return selectRegionByGemini(request);
        }

        String normalizedRegion = region.trim();

        // region 직접 입력: 경기도인지 검증
        if (!isGyeonggiRegion(normalizedRegion)) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "TripPing은 경기도 지역만 추천합니다."
            );
        }

        return normalizedRegion;
    }

    private boolean isGyeonggiRegion(
            String region
    ) {
        List<String> gyeonggiRegions = List.of(
                "경기도",
                "수원",
                "성남",
                "고양",
                "용인",
                "부천",
                "안산",
                "안양",
                "남양주",
                "화성",
                "평택",
                "의정부",
                "시흥",
                "파주",
                "광명",
                "김포",
                "군포",
                "광주",
                "이천",
                "양주",
                "구리",
                "안성",
                "포천",
                "의왕",
                "하남",
                "여주",
                "양평",
                "동두천",
                "과천",
                "가평",
                "연천"
        );

        return gyeonggiRegions.stream()
                .anyMatch(region::contains);
    }

    private boolean isGyeonggiPlace(
            TourismApiResponse.Item item
    ) {
        if (item.getAddr1() == null) {
            return false;
        }

        String address = item.getAddr1()
                .replace(" ", "");

        return address.startsWith("경기도")
                || address.startsWith("경기");
    }

    private String selectRegionByGemini(
            RecommendationRequest request
    ) {
        List<String> candidates =
                candidateService.getCandidates(
                        request.getTravelType()
                );

        String requirement =
                request.getRequirement() == null
                        || request.getRequirement().isBlank()
                        ? "특별한 요구사항 없음"
                        : request.getRequirement();

        String candidateJson;

        try {
            candidateJson =
                    objectMapper.writeValueAsString(candidates);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "지역 후보를 JSON으로 변환할 수 없습니다."
            );
        }

        String prompt = """
                너는 경기도 당일치기 여행 지역 추천 전문가다.

                반드시 경기도 안에서만 여행 지역을 선택해야 한다.
                대한민국의 다른 지역을 선택하면 안 된다.

                여행 조건:
                - 여행 유형: %s
                - 여행자 나이: %d
                - 동행자: %s
                - 추가 요구사항: %s

                선택 가능한 경기도 지역:
                %s

                반드시 위 목록 중 하나만 선택해라.
                새로운 지역을 생성하지 마라.
                반드시 JSON 객체 하나만 반환해라.
                마크다운 코드 블록은 사용하지 마라.

                응답 형식:
                {
                  "region": "선택한 지역",
                  "reason": "선택 이유"
                }
                """.formatted(
                request.getTravelType(),
                request.getAge(),
                request.getCompanion(),
                requirement,
                candidateJson
        );

        String rawResponse =
                geminiApiClient.generateContent(prompt);

        RegionSelectionResponse selection =
                parseRegionSelection(rawResponse);

        if (selection.getRegion() == null
                || !candidates.contains(selection.getRegion())) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "Gemini가 허용되지 않은 경기도 지역을 반환했습니다."
            );
        }

        return selection.getRegion();
    }

    private RegionSelectionResponse parseRegionSelection(
            String rawResponse
    ) {
        try {
            String json = rawResponse
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            return objectMapper.readValue(
                    json,
                    RegionSelectionResponse.class
            );

        } catch (JsonProcessingException exception) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "Gemini 지역 선택 응답을 파싱할 수 없습니다."
            );
        }
    }

    private NearbyRecommendationResponse recommendNearbyByPlaceId(
            String mainPlaceId
    ) {
        String contentId =
                mainPlaceId.replace("tourism-", "");

        TourismApiResponse detailResponse =
                tourismApiClient.getPlaceDetail(contentId);

        PlaceDetailResponse mainPlace =
                tourismPlaceMapper.toPlaceDetailResponse(
                        contentId,
                        detailResponse,
                        null,
                        null
                );

        if (mainPlace == null
                || mainPlace.getLatitude() == null
                || mainPlace.getLongitude() == null) {
            throw new BusinessException(
                    ErrorCode.PLACE_NOT_FOUND
            );
        }

        TourismApiResponse nearbyResponse =
                tourismApiClient.getNearbyPlaces(
                        mainPlace.getLongitude(),
                        mainPlace.getLatitude(),
                        NEARBY_RADIUS_METERS
                );

        List<NearbyRecommendationResponse.NearbyPlace> attractions =
                new ArrayList<>();

        List<NearbyRecommendationResponse.NearbyPlace> cafes =
                new ArrayList<>();

        List<NearbyRecommendationResponse.NearbyPlace> restaurants =
                new ArrayList<>();

        if (nearbyResponse == null
                || nearbyResponse.getResponse() == null
                || nearbyResponse.getResponse().getBody() == null
                || nearbyResponse.getResponse().getBody().getItems() == null
                || nearbyResponse.getResponse()
                .getBody()
                .getItems()
                .getItem() == null) {

            return NearbyRecommendationResponse.builder()
                    .mainPlaceId(mainPlaceId)
                    .attractions(attractions)
                    .cafes(cafes)
                    .restaurants(restaurants)
                    .build();
        }

        for (TourismApiResponse.Item item :
                nearbyResponse.getResponse()
                        .getBody()
                        .getItems()
                        .getItem()) {

            if (contentId.equals(item.getContentid())) {
                continue;
            }

            if (item.getMapx() == null
                    || item.getMapx().isBlank()
                    || item.getMapy() == null
                    || item.getMapy().isBlank()) {
                continue;
            }

            if (isExcludedAccommodation(item)) {
                continue;
            }

            NearbyRecommendationResponse.NearbyPlace place =
                    enrichNearbyPlace(item);

            String contentTypeId = item.getContenttypeid();

            if ("39".equals(contentTypeId)) {
                // 음식점 중 카페 키워드가 있으면 카페로 분류
                if (isCafe(item)) {
                    if (cafes.size() < 20) {
                        cafes.add(place);
                    }
                } else {
                    if (restaurants.size() < 20) {
                        restaurants.add(place);
                    }
                }

            } else {
                if (attractions.size() < 20) {
                    attractions.add(place);
                }
            }

            if (attractions.size() >= 20
                    && cafes.size() >= 20
                    && restaurants.size() >= 20) {
                break;
            }
        }

        return NearbyRecommendationResponse.builder()
                .mainPlaceId(mainPlaceId)
                .attractions(attractions)
                .cafes(cafes)
                .restaurants(restaurants)
                .build();
    }

    public NearbyRecommendationResponse recommendNearby(
            NearbyRecommendationRequest request
    ) {
        return recommendNearbyByPlaceId(
                request.getMainPlaceId()
        );
    }

    private boolean hasValidCoordinates(
            TourismApiResponse.Item item
    ) {
        return item.getMapx() != null
                && !item.getMapx().isBlank()
                && item.getMapy() != null
                && !item.getMapy().isBlank();
    }

    private RecommendationResponse.RecommendedPlace toRecommendedPlace(
            TourismApiResponse.Item item
    ) {
        return RecommendationResponse.RecommendedPlace.builder()
                .placeId("tourism-" + item.getContentid())
                .name(item.getTitle())
                .imageUrl(item.getFirstimage())
                .summary(makeSummary(item))
                .latitude(parseDouble(item.getMapy()))
                .longitude(parseDouble(item.getMapx()))
                .build();
    }

    private String makeSummary(
            TourismApiResponse.Item item
    ) {
        if (item.getOverview() != null
                && !item.getOverview().isBlank()) {
            return item.getOverview();
        }

        return "경기도 " + item.getTitle() + " 관광지입니다.";
    }

    private Double parseDouble(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private List<RecommendationResponse.RecommendedPlace> recommendPlacesByGemini(
            RecommendationRequest request,
            String selectedRegion,
            List<TourismApiResponse.Item> candidateItems
    ) {
        String candidateJson = makeCandidateJson(candidateItems);

        String requirement =
                request.getRequirement() == null
                        || request.getRequirement().isBlank()
                        ? "특별한 요구사항 없음"
                        : request.getRequirement();

        String prompt = """
            너는 경기도 당일치기 여행 관광지 추천 전문가다.

            사용자의 여행 조건을 분석하고,
            제공된 실제 관광지 목록 중 가장 적합한 장소를 정확히 3개 선택해라.

            여행 지역: %s
            여행 유형: %s
            여행자 나이: %d
            동행자: %s
            추가 요구사항: %s

            선택 가능한 실제 관광지 목록:
            %s

            반드시 위 목록에 존재하는 placeId만 사용해라.
            존재하지 않는 관광지를 새로 만들면 안 된다.
            반드시 3개를 선택해라.
            각 장소에 대해 한국어 한 줄 추천 이유를 작성해라.
            반드시 JSON 객체 하나만 반환해라.
            마크다운 코드 블록은 사용하지 마라.

            응답 형식:
            {
              "places": [
                {
                  "placeId": "관광공사 placeId",
                  "summary": "추천 이유"
                },
                {
                  "placeId": "관광공사 placeId",
                  "summary": "추천 이유"
                },
                {
                  "placeId": "관광공사 placeId",
                  "summary": "추천 이유"
                }
              ]
            }
            """.formatted(
                selectedRegion,
                request.getTravelType(),
                request.getAge(),
                request.getCompanion(),
                requirement,
                candidateJson
        );

        String rawResponse =
                geminiApiClient.generateContent(prompt);

        GeminiPlaceRecommendationResponse geminiResponse =
                parseGeminiPlaceRecommendation(rawResponse);

        return mergeGeminiResultWithTourismData(
                geminiResponse,
                candidateItems
        );
    }

    private String makeCandidateJson(
            List<TourismApiResponse.Item> items
    ) {
        List<Map<String, String>> candidates = items.stream()
                .map(item -> java.util.Map.of(
                        "placeId", "tourism-" + item.getContentid(),
                        "name", item.getTitle() == null
                                ? ""
                                : item.getTitle(),
                        "address", item.getAddr1() == null
                                ? ""
                                : item.getAddr1(),
                        "latitude", item.getMapy() == null
                                ? ""
                                : item.getMapy(),
                        "longitude", item.getMapx() == null
                                ? ""
                                : item.getMapx()
                ))
                .toList();

        try {
            return objectMapper.writeValueAsString(candidates);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "관광지 후보를 JSON으로 변환할 수 없습니다."
            );
        }
    }

    private GeminiPlaceRecommendationResponse
    parseGeminiPlaceRecommendation(
            String rawResponse
    ) {
        try {
            String json = extractJson(rawResponse);

            GeminiPlaceRecommendationResponse response =
                    objectMapper.readValue(
                            json,
                            GeminiPlaceRecommendationResponse.class
                    );

            if (response.getPlaces() == null
                    || response.getPlaces().size() != 3) {
                throw new BusinessException(
                        ErrorCode.EXTERNAL_API_ERROR,
                        "Gemini가 관광지 3개를 반환하지 않았습니다."
                );
            }

            return response;

        } catch (JsonProcessingException exception) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "Gemini 관광지 추천 응답을 파싱할 수 없습니다."
            );
        }
    }

    private String extractJson(
            String rawResponse
    ) {
        String json = rawResponse
                .replace("```json", "")
                .replace("```JSON", "")
                .replace("```", "")
                .trim();

        int startIndex = json.indexOf("{");
        int endIndex = json.lastIndexOf("}");

        if (startIndex < 0 || endIndex < startIndex) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "Gemini 응답에서 JSON을 찾을 수 없습니다."
            );
        }

        return json.substring(
                startIndex,
                endIndex + 1
        );
    }

    private List<RecommendationResponse.RecommendedPlace>
    mergeGeminiResultWithTourismData(
            GeminiPlaceRecommendationResponse geminiResponse,
            List<TourismApiResponse.Item> candidateItems
    ) {
        Map<String, TourismApiResponse.Item> candidateMap =
                candidateItems.stream()
                        .collect(java.util.stream.Collectors.toMap(
                                item -> "tourism-" + item.getContentid(),
                                item -> item,
                                (first, second) -> first
                        ));

        List<RecommendationResponse.RecommendedPlace> result =
                new ArrayList<>();

        Set<String> selectedPlaceIds =
                new HashSet<>();

        for (GeminiPlaceRecommendationResponse.RecommendedPlace recommended :
                geminiResponse.getPlaces()) {

            if (recommended == null
                    || recommended.getPlaceId() == null
                    || recommended.getPlaceId().isBlank()) {
                continue;
            }

            String placeId =
                    recommended.getPlaceId().trim();

            if (!selectedPlaceIds.add(placeId)) {
                continue;
            }

            TourismApiResponse.Item item =
                    candidateMap.get(placeId);

            if (item == null) {
                continue;
            }

            result.add(
                    RecommendationResponse.RecommendedPlace.builder()
                            .placeId(
                                    "tourism-" + item.getContentid()
                            )
                            .name(item.getTitle())
                            .imageUrl(item.getFirstimage())
                            .summary(resolveRecommendationSummary(
                                    recommended.getSummary(),
                                    item
                                    )
                            )
                            .latitude(
                                    parseDouble(item.getMapy())
                            )
                            .longitude(
                                    parseDouble(item.getMapx())
                            )
                            .build()
            );

            if (result.size() == 3) {
                break;
            }
        }

        if (result.size() != 3) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "Gemini가 중복되거나 유효하지 않은 관광지를 반환했습니다."
            );
        }
        return result;
    }

    private String resolveRecommendationSummary(
            String geminiSummary,
            TourismApiResponse.Item item
    ) {
        if (geminiSummary != null
                && !geminiSummary.isBlank()) {
            return geminiSummary;
        }

        if (item.getOverview() != null
                && !item.getOverview().isBlank()) {
            return item.getOverview();
        }

        return item.getTitle()
                + " 여행 시 방문하기 좋은 관광지입니다.";
    }

    private boolean isCafe(
            TourismApiResponse.Item item
    ) {
        String title = item.getTitle() == null
                ? ""
                : item.getTitle();

        String overview = item.getOverview() == null
                ? ""
                : item.getOverview();

        String text = (title + " " + overview)
                .toLowerCase();

        List<String> cafeKeywords = List.of(
                "카페",
                "커피",
                "커피숍",
                "커피하우스",
                "베이커리카페",
                "브런치카페",
                "로스터리",
                "cafe",
                "coffee",
                "dessert",
                "베이커리"
        );

        return cafeKeywords.stream()
                .anyMatch(text::contains);
    }

    private String extractAddress(
            TourismApiResponse response
    ) {
        if (response == null
                || response.getResponse() == null
                || response.getResponse().getBody() == null
                || response.getResponse()
                .getBody()
                .getItems() == null
                || response.getResponse()
                .getBody()
                .getItems()
                .getItem() == null
                || response.getResponse()
                .getBody()
                .getItems()
                .getItem()
                .isEmpty()) {
            return null;
        }

        TourismApiResponse.Item item =
                response.getResponse()
                        .getBody()
                        .getItems()
                        .getItem()
                        .get(0);

        return item.getAddr1();
    }

    public CourseResponse generateCourse(
            CourseGenerationRequest request
    ) {
        NearbyRecommendationResponse nearbyResponse =
                recommendNearbyByPlaceId(
                        request.getMainPlaceId()
                );

        validateSelectedPlaceIds(
                nearbyResponse,
                request.getSelectedPlaceIds()
        );

        NearbyRecommendationResponse selectedNearby =
                filterSelectedPlaces(
                        nearbyResponse,
                        request.getSelectedPlaceIds()
                );

        CoursePlaceResponse mainPlace =
                createMainCoursePlace(
                        request.getMainPlaceId()
                );

        String prompt =
                buildCoursePrompt(
                        request,
                        mainPlace,
                        selectedNearby
                );

        String rawResponse =
                geminiApiClient.generateContent(
                        prompt
                );

        GeminiCourseResponse geminiResponse =
                parseCourseResponse(
                        rawResponse
                );

        return mergeCourseResult(
                geminiResponse,
                mainPlace,
                selectedNearby,
                request
        );
    }

    private String buildCoursePrompt(
            CourseGenerationRequest request,
            CoursePlaceResponse mainPlace,
            NearbyRecommendationResponse nearbyResponse
    ) {
        String attractionsJson =
                toPlacesJson(
                        nearbyResponse.getAttractions()
                );

        String cafesJson =
                toPlacesJson(
                        nearbyResponse.getCafes()
                );

        String restaurantsJson =
                toPlacesJson(
                        nearbyResponse.getRestaurants()
                );

        String requirement =
                request.getRequirement() == null
                        || request.getRequirement().isBlank()
                        ? "특별한 요구사항 없음"
                        : request.getRequirement();

        return """
            너는 경기도 %s 당일치기 여행 코스 설계 전문가다.

            사용자의 조건에 맞춰 실제 장소만 사용하는 여행 코스를 만들어라.

            사용자 조건:
            - 여행 지역: %s
            - 여행 유형: %s
            - 여행자 나이: %s
            - 동행자: %s
            - 추가 요구사항: %s

            아래 장소 목록은 관광공사 API에서 조회한 실제 장소다.
            반드시 아래 목록에 존재하는 placeId만 사용해야 한다.
            새로운 장소를 만들거나 장소명을 임의로 변경하지 마라.

            [메인 관광지]
            - placeId: %s
            - name : %s
            
            [사용자가 선택한 주변 관광지]
            %s

            [사용자가 선택한 카페]
            %s

            [사용자가 선택한 음식점]
            %s

            코스 생성 규칙:
            1. 메인 관광지는 반드시 코스에 포함한다.
            2. 반드시 위 장소 목록의 placeId만 사용한다.
            3. 관광지, 카페, 음식점을 여행 조건에 맞게 순서를 조합한다.
            4. 방문 순서를 1부터 지정한다.
            5. 최종 코스의 첫 번재 장소는 항상 메인 관광지다.
            6. 같은 placeId를 중복 사용하지 않는다.
            7. 최소 3개 이상의 장소를 선택한다.
            8. 모든 장소는 입력한 지역 주변 장소 목록에서만 선택한다.
            9. 반드시 JSON 객체 하나만 반환한다.
            10. 마크다운 코드 블록은 사용하지 않는다.
            11. 여행 지역은 반드시 "%s"로 고정한다.
            12. course title에 다른 지역명을 절대 포함하지 않는다.
            13. description과 tags에도 다른 지역명을 포함하지 않는다.
            14. 강릉, 서울, 부산, 제주 등 입력 지역이 아닌 지역을 절대 언급하지 않는다.
            
            응답 형식:
            {
              "title": "코스 제목",
              "totalDuration": "약 6시간",
              "description": "코스 설명",
              "tags": ["자연", "산책", "카페"],
              "places": [
                {
                  "placeId": "tourism-123",
                  "category": "관광지",
                  "visitOrder": "1",
                  "recommendedTime": "10:00~12:00",
                  "reason": "추천 이유"
                }
              ]
            }
            """.formatted(
                request.getRegion(),
                request.getRegion(),
                request.getTravelType(),
                request.getAge(),
                request.getCompanion(),
                requirement,
                mainPlace.getPlaceId(),
                mainPlace.getName(),
                attractionsJson,
                cafesJson,
                restaurantsJson,
                request.getRegion()
        );
    }

    private String toPlacesJson(
            List<NearbyRecommendationResponse.NearbyPlace> places
    ) {
        if (places == null || places.isEmpty()) {
            return "[]";
        }

        List<Map<String, Object>> promptPlaces =
                places.stream()
                        .map(place -> {
                            Map<String, Object> map =
                                    new HashMap<>();

                            map.put(
                                    "placeId",
                                    nullToEmpty(place.getPlaceId())
                            );
                            map.put(
                                    "name",
                                    nullToEmpty(place.getName())
                            );
                            map.put(
                                    "address",
                                    nullToEmpty(place.getAddress())
                            );
                            map.put(
                                    "latitude",
                                    place.getLatitude()
                            );
                            map.put(
                                    "longitude",
                                    place.getLongitude()
                            );

                            return map;
                        })
                        .toList();

        try {
            return objectMapper.writeValueAsString(
                    promptPlaces
            );
        } catch (JsonProcessingException exception) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "코스 생성 장소 목록을 JSON으로 변환할 수 없습니다."
            );
        }
    }

    private String nullToEmpty(
            String value
    ) {
        return value == null ? "" : value;
    }

    private GeminiCourseResponse parseCourseResponse(
            String rawResponse
    ) {
        try {
            String json =
                    extractJsonObject(rawResponse);

            GeminiCourseResponse response =
                    objectMapper.readValue(
                            json,
                            GeminiCourseResponse.class
                    );

            if (response.getPlaces() == null
                    || response.getPlaces().size() < 3) {
                throw new BusinessException(
                        ErrorCode.EXTERNAL_API_ERROR,
                        "Gemini가 3개 이상의 코스 장소를 반환하지 않았습니다."
                );
            }

            return response;

        } catch (JsonProcessingException exception) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "Gemini 코스 응답을 파싱할 수 없습니다."
            );
        }
    }

    private String extractJsonObject(
            String rawResponse
    ) {
        if (rawResponse == null
                || rawResponse.isBlank()) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "Gemini 코스 응답이 비어 있습니다."
            );
        }

        String json = rawResponse
                .replace("```json", "")
                .replace("```JSON", "")
                .replace("```", "")
                .trim();

        int startIndex = json.indexOf("{");
        int endIndex = json.lastIndexOf("}");

        if (startIndex < 0
                || endIndex < startIndex) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "Gemini 응답에서 JSON 객체를 찾을 수 없습니다."
            );
        }

        return json.substring(
                startIndex,
                endIndex + 1
        );
    }

    private List<NearbyRecommendationResponse.NearbyPlace>
    collectCourseCandidates(
            NearbyRecommendationResponse nearbyResponse,
            String mainPlaceId
    ) {
        List<NearbyRecommendationResponse.NearbyPlace> candidates =
                new ArrayList<>();

        candidates.addAll(nearbyResponse.getAttractions());
        candidates.addAll(nearbyResponse.getCafes());
        candidates.addAll(nearbyResponse.getRestaurants());

        return candidates.stream()
                .filter(place ->
                        place.getPlaceId() != null
                                && !place.getPlaceId().equals(mainPlaceId)
                )
                .distinct()
                .toList();
    }

    private CourseResponse mergeCourseResult(
            GeminiCourseResponse geminiResponse,
            CoursePlaceResponse mainPlace,
            NearbyRecommendationResponse selectedNearby,
            CourseGenerationRequest request
    ) {
        Map<String, NearbyRecommendationResponse.NearbyPlace>
                candidateMap =
                createCandidateMap(selectedNearby);

        Set<String> selectedPlaceIds =
                new HashSet<>();

        List<CoursePlaceResponse> coursePlaces =
                new ArrayList<>();

        // 메인 관광지는 항상 1번
        coursePlaces.add(
                CoursePlaceResponse.builder()
                        .order(1)
                        .placeId(mainPlace.getPlaceId())
                        .name(mainPlace.getName())
                        .summary(mainPlace.getSummary())
                        .imageUrl(mainPlace.getImageUrl())
                        .latitude(mainPlace.getLatitude())
                        .longitude(mainPlace.getLongitude())
                        .build()
        );

        // Gemini가 선택한 주변 장소 처리
        for (GeminiCourseResponse.CoursePlace selected :
                geminiResponse.getPlaces()) {

            if (selected == null
                    || selected.getPlaceId() == null
                    || selected.getPlaceId().isBlank()) {
                continue;
            }

            String placeId =
                    selected.getPlaceId().trim();

            // 주변 장소 중복 방지
            if (!selectedPlaceIds.add(placeId)) {
                continue;
            }

            // 메인 관광지를 다시 넣는 경우 방지
            if (mainPlace.getPlaceId().equals(placeId)) {
                continue;
            }

            NearbyRecommendationResponse.NearbyPlace original =
                    candidateMap.get(placeId);

            // 사용자가 선택한 주변 장소가 아니면 제외
            if (original == null) {
                continue;
            }

            coursePlaces.add(
                    CoursePlaceResponse.builder()
                            .order(coursePlaces.size() + 1)
                            .placeId(original.getPlaceId())
                            .name(original.getName())
                            .summary(
                                    buildCoursePlaceSummary(
                                            selected
                                    )
                            )
                            .imageUrl(original.getImageUrl())
                            .latitude(original.getLatitude())
                            .longitude(original.getLongitude())
                            .build()
            );
        }

        if (coursePlaces.size() < 2) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "여행 코스를 구성할 장소가 부족합니다."
            );
        }

        return CourseResponse.builder()
                .courseId(
                        "course-" + System.currentTimeMillis()
                )
                .courseTitle(
                        buildCourseTitle(
                                geminiResponse.getTitle(),
                                request.getRegion()
                        )
                )
                .estimatedDuration(
                        geminiResponse.getTotalDuration()
                )
                .tags(
                        geminiResponse.getTags()
                )
                .description(
                        geminiResponse.getDescription()
                )
                .mapImageUrl(null)
                .places(coursePlaces)
                .build();
    }

    private Map<String, NearbyRecommendationResponse.NearbyPlace>
    createCandidateMap(
            NearbyRecommendationResponse selectedNearby
    ) {
        Map<String, NearbyRecommendationResponse.NearbyPlace>
                candidateMap = new HashMap<>();

        if (selectedNearby == null) {
            return candidateMap;
        }

        addToCandidateMap(
                candidateMap,
                selectedNearby.getAttractions()
        );

        addToCandidateMap(
                candidateMap,
                selectedNearby.getCafes()
        );

        addToCandidateMap(
                candidateMap,
                selectedNearby.getRestaurants()
        );

        return candidateMap;
    }

    private void addToCandidateMap(
            Map<String, NearbyRecommendationResponse.NearbyPlace>
                    candidateMap,
            List<NearbyRecommendationResponse.NearbyPlace> places
    ) {
        if (places == null || places.isEmpty()) {
            return;
        }

        for (NearbyRecommendationResponse.NearbyPlace place :
                places) {

            if (place == null
                    || place.getPlaceId() == null
                    || place.getPlaceId().isBlank()) {
                continue;
            }

            String placeId =
                    place.getPlaceId().trim();

            // 같은 placeId가 여러 리스트에 있으면 첫 번째 장소 유지
            candidateMap.putIfAbsent(
                    placeId,
                    place
            );
        }
    }

    private String buildCoursePlaceSummary(
            GeminiCourseResponse.CoursePlace selected
    ) {
        String reason =
                selected.getReason() == null
                        || selected.getReason().isBlank()
                        ? "여행 조건에 맞는 추천 장소입니다."
                        : selected.getReason();

        String category =
                selected.getCategory() == null
                        || selected.getCategory().isBlank()
                        ? ""
                        : "[" + selected.getCategory() + "] ";

        String time =
                selected.getRecommendedTime() == null
                        || selected.getRecommendedTime().isBlank()
                        ? ""
                        : " 방문 추천 시간: "
                        + selected.getRecommendedTime()
                        + ".";

        return category + reason + time;
    }

    private NearbyRecommendationResponse filterSelectedPlaces(
            NearbyRecommendationResponse nearbyResponse,
            List<String> selectedPlaceIds
    ) {
        Set<String> selectedIds =
                new HashSet<>(
                        selectedPlaceIds
                );

        return NearbyRecommendationResponse.builder()
                .mainPlaceId(
                        nearbyResponse.getMainPlaceId()
                )
                .attractions(
                        filterPlaces(
                                nearbyResponse.getAttractions(),
                                selectedIds
                        )
                )
                .cafes(
                        filterPlaces(
                                nearbyResponse.getCafes(),
                                selectedIds
                        )
                )
                .restaurants(
                        filterPlaces(
                                nearbyResponse.getRestaurants(),
                                selectedIds
                        )
                )
                .build();
    }

    private List<NearbyRecommendationResponse.NearbyPlace>
    filterPlaces(
            List<NearbyRecommendationResponse.NearbyPlace> places,
            Set<String> selectedIds
    ) {
        if (places == null) {
            return List.of();
        }

        return places.stream()
                .filter(Objects::nonNull)
                .filter(place ->
                        place.getPlaceId() != null
                                && selectedIds.contains(
                                place.getPlaceId()
                        )
                )
                .toList();
    }

    private void validateSelectedPlaces(
            NearbyRecommendationResponse nearbyResponse,
            List<String> selectedPlaceIds
    ) {
        Set<String> availableIds =
                new HashSet<>();

        addPlaceIds(
                availableIds,
                nearbyResponse.getAttractions()
        );

        addPlaceIds(
                availableIds,
                nearbyResponse.getCafes()
        );

        addPlaceIds(
                availableIds,
                nearbyResponse.getRestaurants()
        );

        boolean allValid =
                selectedPlaceIds.stream()
                        .allMatch(availableIds::contains);

        if (!allValid) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "선택한 장소 중 주변 추천 목록에 없는 장소가 포함되어 있습니다."
            );
        }
    }

    private void addPlaceIds(
            Set<String> placeIds,
            List<NearbyRecommendationResponse.NearbyPlace> places
    ) {
        if (places == null) {
            return;
        }

        for (NearbyRecommendationResponse.NearbyPlace place :
                places) {

            if (place == null
                    || place.getPlaceId() == null
                    || place.getPlaceId().isBlank()) {
                continue;
            }

            placeIds.add(
                    place.getPlaceId()
            );
        }
    }

    private void validateSelectedPlaceIds(
            NearbyRecommendationResponse nearbyResponse,
            List<String> selectedPlaceIds
    ) {
        if (selectedPlaceIds == null
                || selectedPlaceIds.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "선택한 주변 장소가 없습니다."
            );
        }

        Set<String> availablePlaceIds =
                new HashSet<>();

        addPlaceIds(
                availablePlaceIds,
                nearbyResponse.getAttractions()
        );

        addPlaceIds(
                availablePlaceIds,
                nearbyResponse.getCafes()
        );

        addPlaceIds(
                availablePlaceIds,
                nearbyResponse.getRestaurants()
        );

        Set<String> requestedPlaceIds =
                new HashSet<>(
                        selectedPlaceIds
                );

        if (requestedPlaceIds.size()
                != selectedPlaceIds.size()) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "중복된 장소가 선택되었습니다."
            );
        }

        boolean allExist =
                requestedPlaceIds.stream()
                        .allMatch(
                                availablePlaceIds::contains
                        );

        if (!allExist) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "선택한 장소 중 주변 추천 목록에 없는 장소가 포함되어 있습니다."
            );
        }
    }

    private CoursePlaceResponse createMainCoursePlace(
            String mainPlaceId
    ) {
        String contentId =
                mainPlaceId.replace("tourism-", "");

        TourismApiResponse detailResponse =
                tourismApiClient.getPlaceDetail(contentId);

        PlaceDetailResponse mainPlace =
                tourismPlaceMapper.toPlaceDetailResponse(
                        contentId,
                        detailResponse,
                        null,
                        null
                );

        if (mainPlace == null) {
            throw new BusinessException(
                    ErrorCode.PLACE_NOT_FOUND,
                    "메인 관광지 정보를 찾을 수 없습니다."
            );
        }

        return CoursePlaceResponse.builder()
                .order(1)
                .placeId(mainPlaceId)
                .name(mainPlace.getName())
                .summary("사용자가 선택한 메인 관광지입니다.")
                .imageUrl(mainPlace.getImageUrl())
                .latitude(mainPlace.getLatitude())
                .longitude(mainPlace.getLongitude())
                .build();
    }

    // 제목 변경 시 해당 함수 수정
    private String buildCourseTitle(
            String geminiTitle,
            String region
    ) {
        return region + " 맞춤 당일치기 여행 코스";
    }

    private List<String> normalizeTags(
            List<String> tags,
            String region
    ) {
        if (tags == null) {
            return List.of(
                    "#" + region
            );
        }

        return tags.stream()
                .filter(tag ->
                        tag != null
                                && !containsOtherRegion(
                                tag,
                                region
                        )
                )
                .toList();
    }

    private boolean containsOtherRegion(
            String text,
            String region
    ) {
        List<String> otherRegions = List.of(
                "강릉",
                "서울",
                "부산",
                "제주",
                "인천",
                "대구",
                "광주",
                "대전",
                "전주",
                "속초",
                "춘천"
        );

        return otherRegions.stream()
                .filter(other -> !other.equals(region))
                .anyMatch(text::contains);
    }

    private boolean isExcludedAccommodation(
            TourismApiResponse.Item item
    ) {
        String contentTypeId = item.getContenttypeid();

        // 관광공사 contentTypeId 32 = 숙박
        if ("32".equals(contentTypeId)) {
            return true;
        }

        String text = (
                nullToEmpty(item.getTitle())
                        + " "
                        + nullToEmpty(item.getAddr1())
                        + " "
                        + nullToEmpty(item.getOverview())
        ).toLowerCase();

        List<String> excludedKeywords = List.of(
                "펜션",
                "캠핑장",
                "야영장",
                "글램핑",
                "카라반",
                "오토캠핑",
                "캠프장",
                "민박",
                "리조트",
                "콘도",
                "호텔",
                "모텔",
                "게스트하우스",
                "숙박"
        );

        return excludedKeywords.stream()
                .anyMatch(text::contains);
    }

    private NearbyRecommendationResponse.NearbyPlace enrichNearbyPlace(
            TourismApiResponse.Item item
    ) {
        NearbyRecommendationResponse.NearbyPlace basicPlace =
                tourismPlaceMapper.toNearbyPlace(item);

        if (basicPlace == null
                || item.getContentid() == null
                || item.getContentid().isBlank()) {
            return basicPlace;
        }

        TourismApiResponse detailResponse =
                tourismApiClient.getPlaceDetail(
                        item.getContentid()
                );

        PlaceDetailResponse detail =
                tourismPlaceMapper.toPlaceDetailResponse(
                        item.getContentid(),
                        detailResponse,
                        null,
                        null
                );

        if (detail == null) {
            return basicPlace;
        }

        return NearbyRecommendationResponse.NearbyPlace.builder()
                .placeId(basicPlace.getPlaceId())
                .name(firstNonBlank(
                        detail.getName(),
                        basicPlace.getName()
                ))
                .imageUrl(firstNonBlank(
                        detail.getImageUrl(),
                        basicPlace.getImageUrl()
                ))
                .summary(firstNonBlank(
                        detail.getDescription(),
                        basicPlace.getSummary()
                ))
                .address(firstNonBlank(
                        detail.getAddress(),
                        basicPlace.getAddress()
                ))
                .latitude(firstNonNull(
                        detail.getLatitude(),
                        basicPlace.getLatitude()
                ))
                .longitude(firstNonNull(
                        detail.getLongitude(),
                        basicPlace.getLongitude()
                ))
                .build();
    }

    private String firstNonBlank(
            String first,
            String second
    ) {
        if (first != null && !first.isBlank()) {
            return first;
        }

        return second;
    }

    private Double firstNonNull(
            Double first,
            Double second
    ) {
        return first != null ? first : second;
    }
}