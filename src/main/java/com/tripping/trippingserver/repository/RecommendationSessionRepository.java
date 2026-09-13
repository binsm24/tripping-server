package com.tripping.trippingserver.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.Firestore;
import com.tripping.trippingserver.dto.request.RecommendationRequest;
import com.tripping.trippingserver.dto.response.RecommendationResponse;
import com.tripping.trippingserver.dto.response.NearbyRecommendationResponse;
import com.tripping.trippingserver.exception.BusinessException;
import com.tripping.trippingserver.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.*;

/** Persist the exact recommendations shown to the client across requests and server restarts. */
@Repository
@RequiredArgsConstructor
public class RecommendationSessionRepository {
    private final Firestore firestore;
    private final ObjectMapper objectMapper;

    public record Place(String placeId, String name, String summary, String imageUrl,
                        Double latitude, Double longitude, String category) {
        public NearbyRecommendationResponse.NearbyPlace toNearby() {
            return NearbyRecommendationResponse.NearbyPlace.builder()
                    .placeId(placeId).name(name).summary(summary).imageUrl(imageUrl)
                    .latitude(latitude).longitude(longitude).build();
        }
    }

    public record Session(String id, String region, String travelType, Integer age,
                          String companion, String requirement, List<Place> mainPlaces,
                          Map<String, List<Place>> nearby, long expiresAt) {
        public Place mainPlace(String id) {
            return mainPlaces.stream().filter(p -> p.placeId().equals(id)).findFirst()
                    .orElseThrow(() -> invalid("이 추천 세션에 포함되지 않은 메인 관광지입니다."));
        }
        public NearbyRecommendationResponse nearbyResponse(String mainId) {
            List<Place> places = nearby.get(mainId);
            if (places == null) throw invalid("주변 장소 추천을 먼저 요청해 주세요.");
            return NearbyRecommendationResponse.builder().mainPlaceId(mainId)
                    .attractions(category(places, "attraction"))
                    .cafes(category(places, "cafe"))
                    .restaurants(category(places, "restaurant")).build();
        }
        private List<NearbyRecommendationResponse.NearbyPlace> category(List<Place> places, String category) {
            return places.stream().filter(p -> category.equals(p.category())).map(Place::toNearby).toList();
        }
    }

    public Session create(RecommendationRequest request, String region,
                          List<RecommendationResponse.RecommendedPlace> places) {
        Session session = new Session(UUID.randomUUID().toString(), region,
                request.getTravelType(), request.getAge(), request.getCompanion(), request.getRequirement(),
                places.stream().map(p -> new Place(p.getPlaceId(), p.getName(), p.getSummary(),
                        p.getImageUrl(), p.getLatitude(), p.getLongitude(), "main")).toList(),
                Map.of(), System.currentTimeMillis() + Duration.ofHours(24).toMillis());
        try {
            firestore.collection("recommendationSessions").document(session.id())
                    .set(Map.of("snapshot", objectMapper.writeValueAsString(session))).get();
            return session;
        } catch (Exception e) { throw failure(e); }
    }

    public Session get(String id) {
        if (id == null || id.isBlank()) throw invalid("추천 세션 ID가 필요합니다.");
        try {
            var document = firestore.collection("recommendationSessions").document(id).get().get();
            if (!document.exists()) throw invalid("추천 세션이 없습니다. 여행지 추천부터 다시 시작해 주세요.");
            return decode(document.getString("snapshot"));
        } catch (BusinessException e) { throw e; }
        catch (Exception e) { throw failure(e); }
    }

    public NearbyRecommendationResponse saveNearby(String id, String mainId, NearbyRecommendationResponse response) {
        List<Place> places = new ArrayList<>();
        append(places, response.getAttractions(), "attraction");
        append(places, response.getCafes(), "cafe");
        append(places, response.getRestaurants(), "restaurant");
        try {
            return firestore.runTransaction(transaction -> {
                var ref = firestore.collection("recommendationSessions").document(id);
                var document = transaction.get(ref).get();
                if (!document.exists()) throw invalid("추천 세션이 없습니다.");
                Session original = decode(document.getString("snapshot"));
                original.mainPlace(mainId);
                if (original.nearby().containsKey(mainId)) return original.nearbyResponse(mainId);
                Map<String, List<Place>> nearby = new HashMap<>(original.nearby());
                nearby.put(mainId, places);
                Session updated = new Session(original.id(), original.region(), original.travelType(),
                        original.age(), original.companion(), original.requirement(), original.mainPlaces(),
                        nearby, original.expiresAt());
                transaction.update(ref, "snapshot", objectMapper.writeValueAsString(updated));
                return updated.nearbyResponse(mainId);
            }).get();
        } catch (Exception e) { throw failure(e); }
    }

    private void append(List<Place> result, List<NearbyRecommendationResponse.NearbyPlace> places, String category) {
        if (places == null) return;
        for (var p : places) result.add(new Place(p.getPlaceId(), p.getName(), p.getSummary(),
                p.getImageUrl(), p.getLatitude(), p.getLongitude(), category));
    }

    private Session decode(String json) throws Exception {
        Session session = objectMapper.readValue(json, Session.class);
        if (session.expiresAt() <= System.currentTimeMillis())
            throw invalid("추천 세션이 만료되었습니다. 여행지 추천부터 다시 시작해 주세요.");
        return session;
    }
    private static BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.INVALID_INPUT_VALUE, message);
    }
    private BusinessException failure(Exception e) {
        if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        if (e.getCause() instanceof BusinessException business) return business;
        return new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "추천 세션 저장소 처리에 실패했습니다.");
    }
}
