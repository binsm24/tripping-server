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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

        Map<String, Object> data = Map.of(
                "savedCourseId", savedCourseId,
                "courseId", document.getCourseId(),
                "courseTitle", document.getCourseTitle(),
                "estimatedDuration", document.getEstimatedDuration(),
                "tags", document.getTags(),
                "description", document.getDescription(),
                "mapImageUrl", document.getMapImageUrl(),
                "createdAt", document.getCreatedAt()
        );

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

    private SavedCourseDocument toDocument(
            DocumentSnapshot document
    ) {
        return SavedCourseDocument.builder()
                .savedCourseId(
                        document.getString("savedCourseId")
                )
                .courseId(
                        document.getString("courseId")
                )
                .courseTitle(
                        document.getString("courseTitle")
                )
                .estimatedDuration(
                        document.getString("estimatedDuration")
                )
                .tags((List<String>) document.get("tags"))
                .description(
                        document.getString("description")
                )
                .mapImageUrl(
                        document.getString("mapImageUrl")
                )
                .createdAt(
                        document.getString("createdAt")
                )
                .build();
    }
}
