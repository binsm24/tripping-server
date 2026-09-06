package com.tripping.trippingserver.service;

import com.tripping.trippingserver.dto.request.CourseGenerationRequest;
import com.tripping.trippingserver.dto.response.CourseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final RecommendationService recommendationService;

    public CourseResponse createCourse(
            CourseGenerationRequest request
    ) {
        return recommendationService.generateCourse(request);
    }
}
