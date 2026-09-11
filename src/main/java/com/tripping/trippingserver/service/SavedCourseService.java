package com.tripping.trippingserver.service;

import com.tripping.trippingserver.dto.request.SavedCourseCreateRequest;
import com.tripping.trippingserver.dto.response.SavedCourseDetailResponse;
import com.tripping.trippingserver.dto.response.SavedCourseSummaryResponse;
import com.tripping.trippingserver.dto.response.CoursePlaceResponse;
import com.tripping.trippingserver.exception.BusinessException;
import com.tripping.trippingserver.exception.ErrorCode;
import com.tripping.trippingserver.repository.SavedCourseDocument;
import com.tripping.trippingserver.repository.SavedCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.tripping.trippingserver.repository.CourseDocument;
import com.tripping.trippingserver.repository.CourseRepository;

@Service
@RequiredArgsConstructor
public class SavedCourseService {

    private final SavedCourseRepository savedCourseRepository;
    private final CourseRepository courseRepository;

    public SavedCourseDetailResponse saveCourse(
            String userId,
            SavedCourseCreateRequest request
    ) {
        try {
            CourseDocument course =
                    courseRepository.findById(request.getCourseId())
                            .orElseThrow(() ->
                                    new BusinessException(
                                            ErrorCode.COURSE_NOT_FOUND
                                    )
                            );

            LocalDateTime createdAt =
                    LocalDateTime.now();

            LocalDateTime expiresAt =
                    createdAt.plusMinutes(1);

            SavedCourseDocument savedCourse =
                    SavedCourseDocument.builder()
                            .userId(userId)
                            .courseId(course.getCourseId())
                            .courseTitle(course.getCourseTitle())
                            .estimatedDuration(course.getEstimatedDuration())
                            .tags(course.getTags())
                            .description(course.getDescription())
                            .mapImageUrl(course.getMapImageUrl())
                            .createdAt(createdAt.toString())
                            .expiresAt(expiresAt.toString())
                            .places(course.getPlaces())
                            .build();

            SavedCourseDocument saved =
                    savedCourseRepository.save(savedCourse);

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

    public List<SavedCourseSummaryResponse> getSavedCourses(
            String userId
    ) {
        try {
            return savedCourseRepository.findAllByUserId(userId)
                    .stream()
                    .filter(this::isNotExpired)
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

    private boolean isNotExpired(
            SavedCourseDocument document
    ) {
        if (document.getExpiresAt() == null
                || document.getExpiresAt().isBlank()) {
            return false;
        }

        return LocalDateTime.parse(
                document.getExpiresAt()
        ).isAfter(LocalDateTime.now());
    }

    public SavedCourseDetailResponse getSavedCourse(
            String userId,
            String savedCourseId
    ) {
        try {
            SavedCourseDocument savedCourse =
                    savedCourseRepository
                            .findById(savedCourseId)
                            .orElseThrow(() ->
                                    new BusinessException(
                                            ErrorCode.SAVED_COURSE_NOT_FOUND
                                    )
                            );

            if (!userId.equals(savedCourse.getUserId())) {
                throw new BusinessException(
                        ErrorCode.FORBIDDEN,
                        "해당 저장 코스에 접근할 권한이 없습니다."
                );
            }

            if (!isNotExpired(savedCourse)) {
                savedCourseRepository.delete(
                        savedCourse.getSavedCourseId()
                );

                throw new BusinessException(
                        ErrorCode.SAVED_COURSE_NOT_FOUND
                );
            }

            return toDetailResponse(savedCourse);

        } catch (BusinessException exception) {
            throw exception;

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
        List<CoursePlaceResponse> places =
                document.getPlaces() == null
                        ? List.of()
                        : document.getPlaces()
                        .stream()
                        .map(place ->
                                CoursePlaceResponse.builder()
                                        .order(place.getOrder())
                                        .placeId(place.getPlaceId())
                                        .name(place.getName())
                                        .summary(place.getSummary())
                                        .imageUrl(place.getImageUrl())
                                        .build()
                        )
                        .toList();

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
                .places(places)
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
