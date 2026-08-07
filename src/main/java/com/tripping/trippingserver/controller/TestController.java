package com.tripping.trippingserver.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@Tag(
        name = "Test",
        description = "Swagger 연결 테스트 API"
)
public class TestController {

    @GetMapping
    @Operation(
            summary = "Swagger 연결 확인",
            description = "TripPing Swagger 연동 상태를 확인합니다."
    )
    public String test() {
        return "TripPing Swagger Connected";
    }
}
