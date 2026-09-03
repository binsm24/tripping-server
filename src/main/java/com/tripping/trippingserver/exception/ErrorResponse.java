package com.tripping.trippingserver.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "공통 오류 응답")
public class ErrorResponse {

    @Schema(
            description = "HTTP 상태 코드",
            example = "404"
    )
    private int status;

    @Schema(
            description = "요청 성공 여부",
            example = "false"
    )
    private boolean success;

    @Schema(
            description = "오류 메시지",
            example = "저장된 코스를 찾을 수 없습니다."
    )
    private String message;

    public static ErrorResponse of(
            ErrorCode errorCode
    ) {
        return ErrorResponse.builder()
                .status(errorCode.getStatus().value())
                .success(false)
                .message(errorCode.getMessage())
                .build();
    }

    public static ErrorResponse of(
            ErrorCode errorCode,
            String message
    ) {
        return ErrorResponse.builder()
                .status(errorCode.getStatus().value())
                .success(false)
                .message(message)
                .build();
    }
}
