package com.tripping.trippingserver.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "공통 API 응답")
public class ApiResponse<T> {

    @Schema(
            description = "HTTP 상태 코드",
            example = "200"
    )
    private int status;

    @Schema(
            description = "요청 성공 여부",
            example = "true"
    )
    private boolean success;

    @Schema(
            description = "응답 메시지",
            example = "saved course retrieved successfully"
    )
    private String message;

    private T data;

    public static <T> ApiResponse<T> success(
            int status,
            String message,
            T data
    ) {
        return new ApiResponse<>(
                status,
                true,
                message,
                data
        );
    }
}
