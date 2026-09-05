package com.tripping.trippingserver.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ExecutionException;

@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final Firestore firestore;

    public UserDocument findById(String userId)
            throws InterruptedException, ExecutionException {

        DocumentReference documentReference = firestore
                .collection(FirestoreCollection.USERS)
                .document(userId);

        ApiFuture<DocumentSnapshot> future =
                documentReference.get();

        DocumentSnapshot snapshot = future.get();

        if (!snapshot.exists()) {
            return null;
        }

        return snapshot.toObject(UserDocument.class);
    }

    public void save(UserDocument user)
            throws InterruptedException, ExecutionException {

        DocumentReference documentReference = firestore
                .collection(FirestoreCollection.USERS)
                .document(user.getUserId());

        ApiFuture<WriteResult> future =
                documentReference.set(user);

        future.get();
    }
}