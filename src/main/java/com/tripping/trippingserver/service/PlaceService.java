package com.tripping.trippingserver.service;

import com.tripping.trippingserver.dto.response.PlaceDetailResponse;
import com.tripping.trippingserver.external.tourism.TourismApiClient;
import com.tripping.trippingserver.external.tourism.TourismApiResponse;
import com.tripping.trippingserver.external.tourism.TourismPlaceMapper;
import org.springframework.stereotype.Service;

import com.tripping.trippingserver.exception.BusinessException;
import com.tripping.trippingserver.exception.ErrorCode;

import java.util.List;

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

        //1. 기본 상세정보 조회: detailCommon2
        TourismApiResponse commonResponse = tourismApiClient.getPlaceDetail(contentId);

        if (commonResponse == null
                || commonResponse.getResponse() == null
                || commonResponse.getResponse().getBody() == null
                || commonResponse.getResponse().getBody().getItems() == null
                || commonResponse.getResponse().getBody().getItems().getItem() == null
                || commonResponse.getResponse().getBody().getItems().getItem().isEmpty()) {

            throw new BusinessException(
                    ErrorCode.PLACE_NOT_FOUND
            );

        }

        //2. 기본정보에서 첫 번째 관광지 꺼내기
        List<TourismApiResponse.Item> commonItems =
                commonResponse.getResponse().getBody().getItems().getItem();
        TourismApiResponse.Item commonItem = commonItems.get(0);

        //3. 관광지 유형 확인(ex. 12=관광지)
        String contentTypeId =
                commonItem.getContenttypeid();

        //4. 소개정보 조회: 개방시간, 휴무일, 주차정보
        TourismApiResponse introResponse =
                tourismApiClient.getPlaceIntro(
                        contentId,
                        contentTypeId
                );

        //5. 추가 상세정보 조회: 입장료 등
        TourismApiResponse infoResponse =
                tourismApiClient.getPlaceInfo(
                        contentId,
                        contentTypeId
                );

        //6. 세 API 결과를 하나의 상세 응답으로 변환
        PlaceDetailResponse response =
                tourismPlaceMapper.toPlaceDetailResponse(
                        contentId,
                        commonResponse,
                        introResponse,
                        infoResponse
                );

        if (response == null) {
            throw new BusinessException(
                    ErrorCode.PLACE_NOT_FOUND
            );
        }

        return response;
    }
}