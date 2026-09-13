package com.tripping.trippingserver;

import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.tripping.trippingserver.scheduler.SavedCourseCleanupScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "app.cors.allowed-origins=https://app.example.com"
})
class TrippingServerApplicationTests {
    @MockitoBean FirebaseApp firebaseApp;
    @MockitoBean Firestore firestore;
    @MockitoBean SavedCourseCleanupScheduler cleanupScheduler;
    @Value("${local.server.port}") int port;

    @Test
    void healthIsPublicAndSavedCoursesRequireAuthentication() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var health = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/health"))
                    .GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, health.statusCode());
            assertTrue(health.body().contains("UP"));
            var saved = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/saved-courses"))
                    .GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(401, saved.statusCode());
        }
    }

    @Test
    void corsAllowsConfiguredOriginAndRejectsOthers() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            for (var origin : new String[]{"https://app.example.com", "https://untrusted.example.com"}) {
                var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/courses"))
                        .header("Origin", origin).header("Access-Control-Request-Method", "POST")
                        .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
                if (origin.equals("https://app.example.com")) {
                    assertEquals(200, response.statusCode());
                    assertEquals(origin, response.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
                } else {
                    assertEquals(403, response.statusCode());
                    assertTrue(response.headers().firstValue("Access-Control-Allow-Origin").isEmpty());
                }
            }
        }
    }
}
