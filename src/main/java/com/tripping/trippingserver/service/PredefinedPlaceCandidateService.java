package com.tripping.trippingserver.service;

import com.tripping.trippingserver.exception.BusinessException;
import com.tripping.trippingserver.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PredefinedPlaceCandidateService {

    public List<String> getCandidates(String travelType) {
        if (travelType == null || travelType.isBlank()) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "여행 유형은 필수입니다."
            );
        }

        return switch (travelType) {
            case "자연" -> List.of(
                    "가평",
                    "양평",
                    "포천",
                    "연천",
                    "남양주"
            );

            case "도시" -> List.of(
                    "수원",
                    "성남",
                    "고양",
                    "용인",
                    "부천"
            );

            case "복합" -> List.of(
                    "수원",
                    "용인",
                    "파주",
                    "고양",
                    "남양주"
            );

            default -> throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "지원하지 않는 여행 유형입니다."
            );
        };
    }
}