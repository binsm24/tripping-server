package com.tripping.trippingserver.repository;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CourseDocument {

    private String courseId;
    private String courseTitle;
    private String estimatedDuration;
    private List<String> tags;
    private String description;
    private String mapImageUrl;
    private List<CoursePlaceDocument> places;
    private String createdAt;

    @Getter
    @Builder
    public static class CoursePlaceDocument {

        private Integer order;
        private String placeId;
        private String name;
        private String summary;
        private String imageUrl;
        private Double latitude;
        private Double longitude;
    }
}
