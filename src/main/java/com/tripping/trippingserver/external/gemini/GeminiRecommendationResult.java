package com.tripping.trippingserver.external.gemini;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class GeminiRecommendationResult {

    private String title;
    private List<RecommendationItem> recommendations;

    @Getter
    @NoArgsConstructor
    public static class RecommendationItem {

        private String placeId;
        private String summary;
    }
}
