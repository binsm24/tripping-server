package com.tripping.trippingserver.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    INVALID_REQUEST(
            HttpStatus.BAD_REQUEST,
            "잘못된 요청입니다."
    ),

    INVALID_INPUT_VALUE(
            HttpStatus.BAD_REQUEST,
            "입력값이 올바르지 않습니다."
    ),

    UNAUTHORIZED(
            HttpStatus.UNAUTHORIZED,
            "로그인이 필요합니다."
    ),

    FORBIDDEN(
            HttpStatus.FORBIDDEN,
            "접근 권한이 없습니다."
    ),

    PLACE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "관광지를 찾을 수 없습니다."
    ),

    COURSE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "여행 코스를 찾을 수 없습니다."
    ),

    SAVED_COURSE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "저장된 코스를 찾을 수 없습니다."
    ),

    EXTERNAL_API_ERROR(
            HttpStatus.BAD_GATEWAY,
            "외부 API 요청 중 오류가 발생했습니다."
    ),

    INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "서버 내부 오류가 발생했습니다."
    );

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
