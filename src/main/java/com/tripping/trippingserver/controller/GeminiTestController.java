package com.tripping.trippingserver.controller;

import com.tripping.trippingserver.external.gemini.GeminiApiClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/gemini-test")
@RequiredArgsConstructor
@Tag(
        name = "Gemini Test",
        description = "Gemini API 연결 테스트"
)
public class GeminiTestController {

    private final GeminiApiClient geminiApiClient;

    @PostMapping
    @Operation(
            summary = "Gemini 연결 테스트",
            description = "입력한 프롬프트를 Gemini에 전달하고 응답을 확인합니다."
    )
    public String testGemini(
            @Valid @RequestBody GeminiTestRequest request
    ) {
        return geminiApiClient.generateContent(
                request.getPrompt()
        );
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "Gemini 테스트 요청")
    public static class GeminiTestRequest {

        @NotBlank
        @Schema(
                description = "Gemini에 전달할 프롬프트",
                example = "강릉 안목해변을 한 문장으로 소개해줘."
        )
        private String prompt;
    }
}
