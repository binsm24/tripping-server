package com.tripping.trippingserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.tripping.trippingserver.dto.response.CoursePlaceResponse;
import com.tripping.trippingserver.exception.BusinessException;
import com.tripping.trippingserver.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

@Service
public class CourseMapImageService {
    private final String key;
    private final String origin;
    private final String channel;
    private final ObjectMapper json;
    private final Semaphore captures = new Semaphore(2);

    public CourseMapImageService(
            @Value("${kakao.javascript-key:${KAKAO_JAVASCRIPT_KEY:}}") String key,
            @Value("${course-map.origin:http://localhost:8080}") String origin,
            @Value("${course-map.browser-channel:msedge}") String channel,
            ObjectMapper json) {
        this.key = key; this.origin = origin; this.channel = channel; this.json = json;
    }

    public String render(List<CoursePlaceResponse> places) {
        if (key == null || key.isBlank()) throw error("카카오 지도 JavaScript 키가 설정되지 않았습니다.");
        if (places == null || places.isEmpty() || places.stream().anyMatch(p ->
                p.getLatitude() == null || p.getLongitude() == null ||
                !Double.isFinite(p.getLatitude()) || !Double.isFinite(p.getLongitude()) ||
                Math.abs(p.getLatitude()) > 85 || Math.abs(p.getLongitude()) > 180))
            throw error("지도 이미지 생성에 필요한 장소 좌표가 올바르지 않습니다.");
        if (!captures.tryAcquire()) throw error("지도 이미지 생성 요청이 많습니다. 잠시 후 다시 시도해 주세요.");
        try (Playwright playwright = Playwright.create(new Playwright.CreateOptions()
                .setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")))) {
            var options = new BrowserType.LaunchOptions().setHeadless(true).setTimeout(30000);
            if (!channel.isBlank()) options.setChannel(channel);
            try (Browser browser = playwright.chromium().launch(options);
                 BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                         .setViewportSize(630, 346).setDeviceScaleFactor(1))) {
                Page page = context.newPage();
                page.setDefaultTimeout(30000);
                String html = new ClassPathResource("map/course-map.html")
                        .getContentAsString(StandardCharsets.UTF_8);
                // Only numeric coordinates enter the map; place names and user input never become HTML.
                String points = json.writeValueAsString(places.stream().map(p ->
                        Map.of("lat", p.getLatitude(), "lng", p.getLongitude())).toList());
                html = html.replace("__POINTS__", points).replace("__SDK_KEY__",
                        java.net.URLEncoder.encode(key, StandardCharsets.UTF_8));
                String document = html;
                String pageUrl = origin.replaceAll("/$", "") + "/__course-map-render";
                page.route(pageUrl, route -> route.fulfill(new Route.FulfillOptions()
                        .setContentType("text/html; charset=utf-8").setBody(document)));
                page.navigate(pageUrl);
                page.waitForFunction("() => window.mapReady === true || Boolean(window.mapError)");
                if (Boolean.TRUE.equals(page.evaluate("Boolean(window.mapError)")))
                    throw error("카카오 지도 로딩에 실패했습니다. JavaScript 키와 웹 도메인 설정을 확인해 주세요.");
                byte[] image = page.locator("#map").screenshot();
                String dataUrl = "data:image/png;base64," + Base64.getEncoder().encodeToString(image);
                if (dataUrl.length() > 700000) throw error("지도 이미지 크기가 저장 한도를 초과했습니다.");
                return dataUrl;
            }
        } catch (BusinessException e) { throw e; }
        catch (Exception e) {
            throw error("카카오 지도 캡처에 실패했습니다. 브라우저 설치와 지도 키·도메인 설정을 확인해 주세요.");
        } finally { captures.release(); }
    }
    private BusinessException error(String message) {
        return new BusinessException(ErrorCode.EXTERNAL_API_ERROR, message);
    }
}
