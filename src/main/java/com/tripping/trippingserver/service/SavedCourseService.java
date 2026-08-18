package com.tripping.trippingserver.service;

import com.tripping.trippingserver.dto.request.SavedCourseCreateRequest;
import com.tripping.trippingserver.dto.response.SavedCourseDetailResponse;
import com.tripping.trippingserver.dto.response.SavedCourseSummaryResponse;
import com.tripping.trippingserver.exception.BusinessException;
import com.tripping.trippingserver.exception.ErrorCode;
import com.tripping.trippingserver.repository.SavedCourseDocument;
import com.tripping.trippingserver.repository.SavedCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class SavedCourseService {

    private final SavedCourseRepository savedCourseRepository;

    public SavedCourseDetailResponse saveCourse(
            SavedCourseCreateRequest request
    ) {
        SavedCourseDocument document =
                SavedCourseDocument.builder()
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
                        .mapImageUrl(
                                "https://example.com/course-map.png"
                        )
                        .createdAt(
                                LocalDateTime.now().toString()
                        )
                        .build();

        try {
            SavedCourseDocument saved =
                    savedCourseRepository.save(document);

            return toDetailResponse(saved);

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR
            );

        } catch (ExecutionException exception) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR
            );
        }
    }

    public List<SavedCourseSummaryResponse> getSavedCourses(String savedCourseId) {
        try {
            return savedCourseRepository.findAll()
                    .stream()
                    .map(this::toSummaryResponse)
                    .toList();

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR
            );

        } catch (ExecutionException exception) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR
            );
        }
    }

    public SavedCourseDetailResponse getSavedCourse(
            String savedCourseId
    ) {
        try {
            SavedCourseDocument saved =
                    savedCourseRepository
                            .findById(savedCourseId)
                            .orElseThrow(() ->
                                    new BusinessException(
                                            ErrorCode.SAVED_COURSE_NOT_FOUND
                                    )
                            );

            return toDetailResponse(saved);

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR
            );

        } catch (ExecutionException exception) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR
            );
        }
    }

    private SavedCourseDetailResponse toDetailResponse(
            SavedCourseDocument document
    ) {
        return SavedCourseDetailResponse.builder()
                .savedCourseId(document.getSavedCourseId())
                .courseId(document.getCourseId())
                .courseTitle(document.getCourseTitle())
                .estimatedDuration(document.getEstimatedDuration())
                .tags(document.getTags())
                .description(document.getDescription())
                .mapImageUrl(document.getMapImageUrl())
                .createdAt(
                        LocalDateTime.parse(document.getCreatedAt())
                )
                .places(List.of())
                .build();
    }

    private SavedCourseSummaryResponse toSummaryResponse(
            SavedCourseDocument document
    ) {
        return SavedCourseSummaryResponse.builder()
                .savedCourseId(document.getSavedCourseId())
                .courseId(document.getCourseId())
                .courseTitle(document.getCourseTitle())
                .estimatedDuration(document.getEstimatedDuration())
                .mapImageUrl(document.getMapImageUrl())
                .createdAt(
                        LocalDateTime.parse(document.getCreatedAt())
                )
                .build();
    }
}
