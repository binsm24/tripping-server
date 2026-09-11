package com.tripping.trippingserver.scheduler;

import com.tripping.trippingserver.repository.SavedCourseDocument;
import com.tripping.trippingserver.repository.SavedCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SavedCourseCleanupScheduler {

    private final SavedCourseRepository savedCourseRepository;

    @Scheduled(
            fixedDelayString =
                    "${saved-course.cleanup-delay-ms:86400000}"
    )
    public void deleteExpiredCourses() {
        try {
            LocalDateTime now =
                    LocalDateTime.now();

            List<SavedCourseDocument> expiredCourses =
                    savedCourseRepository.findExpiredCourses(now);

            for (SavedCourseDocument course :
                    expiredCourses) {
                savedCourseRepository.delete(
                        course.getSavedCourseId()
                );
            }

            if (!expiredCourses.isEmpty()) {
                System.out.println(
                        "[SavedCourseCleanup] deleted: "
                                + expiredCourses.size()
                );
            }

        } catch (Exception exception) {
            System.err.println(
                    "[SavedCourseCleanup] failed: "
                            + exception.getMessage()
            );
        }
    }
}