package com.tripping.trippingserver.repository;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Repository
@RequiredArgsConstructor
public class CourseRepository {

    private final Firestore firestore;

    private CollectionReference collection() {
        return firestore.collection(
                FirestoreCollection.COURSES
        );
    }

    public CourseDocument save(
            CourseDocument course
    ) throws ExecutionException, InterruptedException {

        String courseId = course.getCourseId();

        if (courseId == null || courseId.isBlank()) {
            throw new IllegalArgumentException(
                    "courseId는 필수입니다."
            );
        }

        Map<String, Object> data = new HashMap<>();

        data.put("courseId", course.getCourseId());
        data.put("courseTitle", course.getCourseTitle());
        data.put("estimatedDuration", course.getEstimatedDuration());
        data.put("tags", course.getTags());
        data.put("description", course.getDescription());
        data.put("mapImageUrl", course.getMapImageUrl());
        data.put("places", toPlaceMaps(course.getPlaces()));
        data.put("createdAt", course.getCreatedAt());

        collection()
                .document(courseId)
                .set(data)
                .get();

        return course;
    }

    public Optional<CourseDocument> findById(
            String courseId
    ) throws ExecutionException, InterruptedException {

        DocumentSnapshot document =
                collection()
                        .document(courseId)
                        .get()
                        .get();

        if (!document.exists()) {
            return Optional.empty();
        }

        return Optional.of(toCourseDocument(document));
    }

    private List<Map<String, Object>> toPlaceMaps(
            List<CourseDocument.CoursePlaceDocument> places
    ) {
        if (places == null) {
            return List.of();
        }

        return places.stream()
                .map(place -> {
                    Map<String, Object> data = new HashMap<>();

                    data.put("order", place.getOrder());
                    data.put("placeId", place.getPlaceId());
                    data.put("name", place.getName());
                    data.put("summary", place.getSummary());
                    data.put("imageUrl", place.getImageUrl());
                    data.put("latitude", place.getLatitude());
                    data.put("longitude", place.getLongitude());

                    return data;
                })
                .toList();
    }

    @SuppressWarnings("unchecked")
    private CourseDocument toCourseDocument(
            DocumentSnapshot document
    ) {
        List<Map<String, Object>> placeMaps =
                (List<Map<String, Object>>) document.get("places");

        List<CourseDocument.CoursePlaceDocument> places =
                placeMaps == null
                        ? List.of()
                        : placeMaps.stream()
                        .map(place ->
                                CourseDocument.CoursePlaceDocument
                                        .builder()
                                        .order(
                                                ((Long) place.get("order"))
                                                        .intValue()
                                        )
                                        .placeId(
                                                (String) place.get("placeId")
                                        )
                                        .name(
                                                (String) place.get("name")
                                        )
                                        .summary(
                                                (String) place.get("summary")
                                        )
                                        .imageUrl(
                                                (String) place.get("imageUrl")
                                        )
                                        .latitude(
                                                toDouble(place.get("latitude"))
                                        )
                                        .longitude(
                                                toDouble(place.get("longitude"))
                                        )
                                        .build()
                        )
                        .toList();

        return CourseDocument.builder()
                .courseId(document.getString("courseId"))
                .courseTitle(document.getString("courseTitle"))
                .estimatedDuration(
                        document.getString("estimatedDuration")
                )
                .tags((List<String>) document.get("tags"))
                .description(document.getString("description"))
                .mapImageUrl(document.getString("mapImageUrl"))
                .places(places)
                .createdAt(document.getString("createdAt"))
                .build();
    }

    private Double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }

        return null;
    }
}
