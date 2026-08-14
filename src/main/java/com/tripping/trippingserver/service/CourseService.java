package com.tripping.trippingserver.service;

import com.tripping.trippingserver.dto.request.CourseCreateRequest;
import com.tripping.trippingserver.dto.response.CoursePlaceResponse;
import com.tripping.trippingserver.dto.response.CourseResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseService {

    public CourseResponse createCourse(
            CourseCreateRequest request
    ) {
        return CourseResponse.builder()
                .courseId(1L)
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
                .places(List.of(
                        CoursePlaceResponse.builder()
                                .order(1)
                                .placeId("101")
                                .name("안목해변")
                                .summary("푸른 바다를 바라보며 산책하기 좋은 해변입니다.")
                                .imageUrl("https://example.com/anmok.jpg")
                                .latitude(37.751853)
                                .longitude(128.896057)
                                .build(),

                        CoursePlaceResponse.builder()
                                .order(2)
                                .placeId("401")
                                .name("테라로사")
                                .summary("감성적인 분위기의 스페셜티 카페입니다.")
                                .imageUrl("https://example.com/terarosa.jpg")
                                .latitude(37.7225)
                                .longitude(128.9304)
                                .build(),

                        CoursePlaceResponse.builder()
                                .order(3)
                                .placeId("201")
                                .name("경포호")
                                .summary("호수를 따라 산책하기 좋은 관광지입니다.")
                                .imageUrl("https://example.com/gyeongpo.jpg")
                                .latitude(37.7982)
                                .longitude(128.8969)
                                .build(),

                        CoursePlaceResponse.builder()
                                .order(4)
                                .placeId("301")
                                .name("초당순두부")
                                .summary("강릉 대표 순두부 맛집입니다.")
                                .imageUrl("https://example.com/tofu.jpg")
                                .latitude(37.7868)
                                .longitude(128.9166)
                                .build()
                ))
                .build();

    }
}
