package com.tripping.trippingserver.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.Base64;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.project-id}")
    private String projectId;

    @Value("${firebase.service-account-path}")
    private String serviceAccountPath;

    @Value("${firebase.service-account-base64:}")
    private String serviceAccountBase64;

    @Bean(destroyMethod = "delete")
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials())
                .setProjectId(projectId)
                .build();
        return FirebaseApp.initializeApp(options);
    }

    private GoogleCredentials credentials() throws IOException {
        if (!serviceAccountBase64.isBlank()) {
            try (var input = new ByteArrayInputStream(Base64.getDecoder().decode(serviceAccountBase64))) {
                return GoogleCredentials.fromStream(input);
            }
        }
        if (!serviceAccountPath.isBlank()) {
            try (var input = new FileInputStream(serviceAccountPath)) {
                return GoogleCredentials.fromStream(input);
            }
        }
        return GoogleCredentials.getApplicationDefault();
    }

    @Bean
    public Firestore firestore(
            FirebaseApp firebaseApp
    ) {
        return FirestoreClient.getFirestore(firebaseApp);
    }
}
