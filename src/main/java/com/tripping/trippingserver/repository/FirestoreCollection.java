package com.tripping.trippingserver.repository;

public final class FirestoreCollection {

    private FirestoreCollection() {
    }

    public static final String USERS = "users";
    public static final String RECOMMENDATION_SESSIONS =
            "recommendationSessions";
    public static final String PLACES = "places";
    public static final String COURSES = "courses";
    public static final String SAVED_COURSES = "savedCourses";
}
