package com.tripping.trippingserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripping.trippingserver.dto.request.CourseGenerationRequest;
import com.tripping.trippingserver.dto.request.RecommendationRequest;
import com.tripping.trippingserver.dto.response.PlaceDetailResponse;
import com.tripping.trippingserver.external.gemini.GeminiApiClient;
import com.tripping.trippingserver.external.groq.GroqApiClient;
import com.tripping.trippingserver.external.tourism.TourismApiClient;
import com.tripping.trippingserver.external.tourism.TourismPlaceMapper;
import com.tripping.trippingserver.repository.CourseRepository;
import com.tripping.trippingserver.service.PredefinedPlaceCandidateService;
import com.tripping.trippingserver.service.RecommendationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import com.tripping.trippingserver.repository.RecommendationSessionRepository;
import com.tripping.trippingserver.repository.RecommendationSessionRepository.Place;
import com.tripping.trippingserver.repository.RecommendationSessionRepository.Session;
import com.tripping.trippingserver.service.CourseMapImageService;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecommendationFlowTests {
    private final ObjectMapper json = new ObjectMapper();
    private final TourismApiClient tourism = mock(TourismApiClient.class);
    private final TourismPlaceMapper mapper = mock(TourismPlaceMapper.class);
    private final GeminiApiClient gemini = mock(GeminiApiClient.class);
    private final GroqApiClient groq = mock(GroqApiClient.class);
    private final CourseRepository courses = mock(CourseRepository.class);
    private final RecommendationSessionRepository sessions = mock(RecommendationSessionRepository.class);
    private final CourseMapImageService maps = mock(CourseMapImageService.class);
    private final RecommendationService service = new RecommendationService(
            tourism, mapper, gemini, groq,
            new PredefinedPlaceCandidateService(), json, courses, sessions, maps);

    @BeforeEach
    void snapshot() {
        when(sessions.get("test")).thenReturn(new Session("test", "수원", "도시", 25, "친구", "산책",
                List.of(new Place("tourism-125555", "수원 화성", "Groq 메인 한줄평", null, 37.0, 127.0, "main")),
                Map.of("tourism-125555", List.of(
                        new Place("tourism-2", "카페", "Groq 카페 한줄평", null, 37.01, 127.01, "cafe"),
                        new Place("tourism-3", "식당", "Groq 식당 한줄평", null, 37.02, 127.02, "restaurant"))),
                Long.MAX_VALUE));
        when(maps.render(anyList())).thenReturn("data:image/png;base64,test");
    }

    @Test
    void citySuffixIsRemovedBeforeTourismSearch() throws Exception {
        RecommendationRequest request = json.readValue("""
                {"region":"경기도 수원시","travelType":"도시","age":25,"companion":"친구"}
                """, RecommendationRequest.class);
        assertThrows(com.tripping.trippingserver.exception.BusinessException.class,
                () -> service.recommend(request));
        verify(tourism).searchPlacesByKeyword("수원");
    }

    @Test
    void emptySelectionCreatesAndPersistsMainOnlyCourseWithoutNearbyLookup() throws Exception {
        CourseGenerationRequest request = json.readValue("""
                {"mainPlaceId":"tourism-125555","recommendationSessionId":"test",
                 "region":"수원","travelType":"도시","age":25,"companion":"친구",
                 "selectedPlaceIds":[]}
                """, CourseGenerationRequest.class);
        when(mapper.toPlaceDetailResponse(eq("125555"), isNull(), isNull(), isNull()))
                .thenReturn(PlaceDetailResponse.builder().name("수원 화성")
                        .description("역사 관광지").latitude(37.0).longitude(127.0).build());
        when(gemini.generateContent(anyString())).thenReturn("""
                {"title":"수원 여행","totalDuration":"약 2시간","description":"역사 산책",
                 "tags":["역사"],"places":[{"placeId":"tourism-125555","visitOrder":"1"}]}
                """);
        var result = service.generateCourse(request);
        assertEquals(1, result.getPlaces().size());
        assertEquals("tourism-125555", result.getPlaces().get(0).getPlaceId());
        verify(courses).save(argThat(course -> course.getPlaces().size() == 1));
        assertEquals("Groq 메인 한줄평", result.getPlaces().get(0).getSummary());
        assertEquals("data:image/png;base64,test", result.getMapImageUrl());
        verifyNoInteractions(tourism);
        verifyNoInteractions(groq);
    }

    @Test
    void omittedSelectionsAreRestoredAndSessionRegionOverridesClient() throws Exception {
        CourseGenerationRequest request = json.readValue("""
                {"mainPlaceId":"tourism-125555","recommendationSessionId":"test",
                 "region":"없음","travelType":"자연","selectedPlaceIds":["tourism-2","tourism-3"]}
                """, CourseGenerationRequest.class);
        when(gemini.generateContent(anyString())).thenReturn("""
                {"title":"여행","totalDuration":"약 4시간","description":"산책",
                 "tags":["힐링"],"places":[{"placeId":"tourism-3"},{"placeId":"unknown"},{"placeId":"tourism-3"}]}
                """);
        var result = service.generateCourse(request);
        assertEquals(List.of("tourism-125555", "tourism-3", "tourism-2"),
                result.getPlaces().stream().map(p -> p.getPlaceId()).toList());
        assertEquals(List.of(1, 2, 3), result.getPlaces().stream().map(p -> p.getOrder()).toList());
        assertEquals("Groq 카페 한줄평", result.getPlaces().get(2).getSummary());
        assertTrue(result.getCourseTitle().startsWith("수원"));
        assertTrue(result.getTags().contains("도시"));
        assertFalse(result.getTags().contains("없음"));
        verify(gemini).generateContent(argThat(prompt -> prompt.contains("수원") && !prompt.contains("없음")));
        verifyNoInteractions(tourism, groq);
    }

    @Test
    void mainPlaceFromAnotherSessionIsRejectedBeforeExternalCalls() throws Exception {
        var request = CourseGenerationRequest.builder().mainPlaceId("tourism-other")
                .recommendationSessionId("test").selectedPlaceIds(List.of()).build();
        assertThrows(com.tripping.trippingserver.exception.BusinessException.class,
                () -> service.generateCourse(request));
        verifyNoInteractions(tourism, groq, gemini, courses, maps);
    }

    @Test
    void sessionSnapshotRoundTripPreservesSummariesAndNearbyCategories() throws Exception {
        Session original = sessions.get("test");
        Session restored = json.readValue(json.writeValueAsString(original), Session.class);
        assertEquals(original, restored);
        assertEquals("Groq 카페 한줄평", restored.nearbyResponse("tourism-125555").getCafes().get(0).getSummary());
    }
}
