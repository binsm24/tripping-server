package com.tripping.trippingserver.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlaceCandidate {

    private String placeId;
    private String name;
    private String category;
    private String description;
    private String imageUrl;
    private Double latitude;
    private Double longitude;
}
