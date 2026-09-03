package com.tripping.trippingserver.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Repository
@RequiredArgsConstructor
public class SavedCourseRepository {

    private final Firestore firestore;

    private CollectionReference collection() {
        return firestore.collection(
                FirestoreCollection.SAVED_COURSES
        );
    }

    public SavedCourseDocument save(
            SavedCourseDocument document
    ) throws ExecutionException, InterruptedException {

        DocumentReference documentReference =
                collection().document();

        String savedCourseId = documentReference.getId();

        Map<String, Object> data = new HashMap<>();

        data.put("savedCourseId", savedCourseId);
        data.put("userId", document.getUserId());
        data.put("courseId", document.getCourseId());
        data.put("courseTitle", document.getCourseTitle());
        data.put("estimatedDuration", document.getEstimatedDuration());
        data.put("tags", document.getTags());
        data.put("description", document.getDescription());
        data.put("mapImageUrl", document.getMapImageUrl());
        data.put("createdAt", document.getCreatedAt());
        data.put("places", toPlaceMaps(document.getPlaces()));


        ApiFuture<WriteResult> future =
                documentReference.set(data);

        future.get();

        return SavedCourseDocument.builder()
                .savedCourseId(savedCourseId)
                .courseId(document.getCourseId())
                .courseTitle(document.getCourseTitle())
                .estimatedDuration(document.getEstimatedDuration())
                .tags(document.getTags())
                .description(document.getDescription())
                .mapImageUrl(document.getMapImageUrl())
                .createdAt(document.getCreatedAt())
                .build();
    }

    public List<SavedCourseDocument> findAll()
            throws ExecutionException, InterruptedException {

        ApiFuture<QuerySnapshot> future =
                collection().get();

        QuerySnapshot querySnapshot = future.get();

        List<SavedCourseDocument> result =
                new ArrayList<>();

        for (DocumentSnapshot document :
                querySnapshot.getDocuments()) {

            result.add(toDocument(document));
        }

        return result;
    }

    public List<SavedCourseDocument> findAllByUserId(
            String userId
    ) throws ExecutionException, InterruptedException {

        QuerySnapshot querySnapshot =
                collection()
                        .whereEqualTo("userId", userId)
                        .get()
                        .get();

        List<SavedCourseDocument> result =
                new ArrayList<>();

        for (DocumentSnapshot document :
                querySnapshot.getDocuments()) {
            result.add(toDocument(document));
        }

        return result;
    }

    public Optional<SavedCourseDocument> findById(
            String savedCourseId
    ) throws ExecutionException, InterruptedException {

        DocumentSnapshot document =
                collection()
                        .document(savedCourseId)
                        .get()
                        .get();

        if (!document.exists()) {
            return Optional.empty();
        }

        return Optional.of(toDocument(document));
    }

    @SuppressWarnings("unchecked")
    private SavedCourseDocument toDocument(
            DocumentSnapshot document
    ) {
        List<Map<String, Object>> placeMaps =
                (List<Map<String, Object>>) document.get("places");

        List<CourseDocument.CoursePlaceDocument> places =
                placeMaps == null
                        ? List.of()
                        : placeMaps.stream()
                        .map(place ->
                                CourseDocument.CoursePlaceDocument.builder()
                                        .order(toInteger(place.get("order")))
                                        .placeId((String) place.get("placeId"))
                                        .name((String) place.get("name"))
                                        .summary((String) place.get("summary"))
                                        .imageUrl((String) place.get("imageUrl"))
                                        .latitude(toDouble(place.get("latitude")))
                                        .longitude(toDouble(place.get("longitude")))
                                        .build()
                        )
                        .toList();

        return SavedCourseDocument.builder()
                .savedCourseId(document.getString("savedCourseId"))
                .userId(document.getString("userId"))
                .courseId(document.getString("courseId"))
                .courseTitle(document.getString("courseTitle"))
                .estimatedDuration(document.getString("estimatedDuration"))
                .tags((List<String>) document.get("tags"))
                .description(document.getString("description"))
                .mapImageUrl(document.getString("mapImageUrl"))
                .createdAt(document.getString("createdAt"))
                .places(places)
                .build();
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }

        return null;
    }

    private Double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }

        return null;
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

                    // 원본 코스 데이터 보존용
                    data.put("latitude", place.getLatitude());
                    data.put("longitude", place.getLongitude());

                    return data;
                })
                .toList();
    }
}
