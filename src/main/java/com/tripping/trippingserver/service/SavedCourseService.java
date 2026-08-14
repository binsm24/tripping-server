package com.tripping.trippingserver.service;

import com.tripping.trippingserver.dto.request.SavedCourseCreateRequest;
import com.tripping.trippingserver.dto.response.CoursePlaceResponse;
import com.tripping.trippingserver.dto.response.SavedCourseDetailResponse;
import com.tripping.trippingserver.dto.response.SavedCourseSummaryResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SavedCourseService {

    private final List<SavedCourseDetailResponse> savedCourses =
            new ArrayList<>();

    public SavedCourseDetailResponse saveCourse(
            SavedCourseCreateRequest request
    ) {
        long savedCourseId = savedCourses.size() + 1L;

        SavedCourseDetailResponse savedCourse =
                SavedCourseDetailResponse.builder()
                        .savedCourseId(savedCourseId)
                        .courseId(request.getCourseId())
                        .courseTitle("강릉 감성 힐링 여행")
                        .estimatedDuration("약 6시간")
                        .tags(List.of(
                                "#자연",
                                "#연인",
                                "#강릉",
                                "#힐링"
                        ))
                        .description(
                                "푸른 바다와 감성 카페를 함께 즐길 수 있는 하루 코스입니다."
                        )
                        .mapImageUrl("https://example.com/course-map.png")
                        .createdAt(LocalDateTime.now())
                        .places(createTemporaryPlaces())
                        .build();

        savedCourses.add(savedCourse);

        return savedCourse;
    }

    public List<SavedCourseSummaryResponse> getSavedCourses() {
        return savedCourses.stream()
                .map(course ->
                        SavedCourseSummaryResponse.builder()
                                .savedCourseId(course.getSavedCourseId())
                                .courseId(course.getCourseId())
                                .courseTitle(course.getCourseTitle())
                                .estimatedDuration(course.getEstimatedDuration())
                                .mapImageUrl(course.getMapImageUrl())
                                .createdAt(course.getCreatedAt())
                                .build()
                )
                .toList();
    }

    public SavedCourseDetailResponse getSavedCourse(
            Long savedCourseId
    ) {
        return savedCourses.stream()
                .filter(course ->
                        course.getSavedCourseId().equals(savedCourseId)
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "저장된 코스를 찾을 수 없습니다."
                        )
                );
    }

    private List<CoursePlaceResponse> createTemporaryPlaces() {
        return List.of(
                CoursePlaceResponse.builder()
                        .order(1)
                        .placeId("101")
                        .name("안목해변")
                        .summary("푸른 바다를 바라보며 산책하기 좋은 해변")
                        .imageUrl("https://example.com/anmok.jpg")
                        .build(),

                CoursePlaceResponse.builder()
                        .order(2)
                        .placeId("401")
                        .name("테라로사")
                        .summary("감성적인 분위기의 스페셜티 카페")
                        .imageUrl("https://example.com/terarosa.jpg")
                        .build(),

                CoursePlaceResponse.builder()
                        .order(3)
                        .placeId("201")
                        .name("경포호")
                        .summary("호수를 따라 산책하기 좋은 관광지")
                        .imageUrl("https://example.com/gyeongpo.jpg")
                        .build(),

                CoursePlaceResponse.builder()
                        .order(4)
                        .placeId("301L")
                        .name("초당순두부")
                        .summary("강릉 대표 순두부 맛집")
                        .imageUrl("https://example.com/tof u.jpg")
                        .build()
        );
    }
}
