package com.tripping.trippingserver.service;

import com.tripping.trippingserver.dto.response.PlaceDetailResponse;
import com.tripping.trippingserver.external.tourism.TourismApiClient;
import com.tripping.trippingserver.external.tourism.TourismApiResponse;
import com.tripping.trippingserver.external.tourism.TourismPlaceMapper;
import org.springframework.stereotype.Service;

@Service
public class PlaceService {

    private final TourismApiClient tourismApiClient;
    private final TourismPlaceMapper tourismPlaceMapper;

    public PlaceService(
            TourismApiClient tourismApiClient,
            TourismPlaceMapper tourismPlaceMapper
    ) {
        this.tourismApiClient = tourismApiClient;
        this.tourismPlaceMapper = tourismPlaceMapper;
    }

    public PlaceDetailResponse getPlaceDetail(String placeId) {

        String contentId = placeId.replace("tourism-", "");

        TourismApiResponse apiResponse =
                tourismApiClient.getPlaceDetail(contentId);

        return tourismPlaceMapper.toPlaceDetailResponse(
                contentId,
                apiResponse
        );
    }
}