package com.tripping.trippingserver.dto.gemini;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class GeminiCourseResponse {

    private String title;
    private String totalDuration;
    private String description;
    private List<String> tags;
    private List<CoursePlace> places;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CoursePlace {

        private String placeId;
        private String category;
        private String visitOrder;
        private String recommendedTime;
        private String reason;
    }
}