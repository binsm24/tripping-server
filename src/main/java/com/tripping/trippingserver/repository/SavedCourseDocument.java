package com.tripping.trippingserver.repository;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SavedCourseDocument {

    private String savedCourseId;
    private String userId;
    private String courseId;
    private String courseTitle;
    private String estimatedDuration;
    private List<String> tags;
    private String description;
    private String mapImageUrl;
    private String createdAt;
    private String expiresAt;
    private List<CourseDocument.CoursePlaceDocument> places;
}