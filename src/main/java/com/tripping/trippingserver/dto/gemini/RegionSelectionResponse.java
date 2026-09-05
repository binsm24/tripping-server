package com.tripping.trippingserver.dto.gemini;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegionSelectionResponse {

    private String region;
    private String reason;
}
