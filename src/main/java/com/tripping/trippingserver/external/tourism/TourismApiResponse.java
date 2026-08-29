package com.tripping.trippingserver.external.tourism;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TourismApiResponse {

    private Response response;

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response {
        private Header header;
        private Body body;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {
        private String resultCode;
        private String resultMsg;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        private Items items;
        private Integer numOfRows;
        private Integer pageNo;
        private Integer totalCount;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Items {
        private List<Item> item;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {

        private String contentid;
        private String contenttypeid;

        private String title;

        private String addr1;
        private String addr2;

        private String firstimage;

        private String mapx;
        private String mapy;

        private String tel;
        private String overview;

        private String dist;
    }
}