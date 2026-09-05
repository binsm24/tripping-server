package com.tripping.trippingserver.dto.gemini;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class GeminiPlaceRecommendationResponse {

    private List<RecommendedPlace> places;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RecommendedPlace {

        private String placeId;
        private String summary;
    }
}