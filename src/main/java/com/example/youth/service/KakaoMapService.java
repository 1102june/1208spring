package com.example.youth.service;

import com.example.youth.dto.kakao.KakaoGeocodingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class KakaoMapService {

    private final WebClient webClient;

    @Value("${kakao.map.rest-api-key}")
    private String restApiKey;

    public KakaoMapService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://dapi.kakao.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 주소를 좌표로 변환 (지오코딩)
     * @param address 주소
     * @return 좌표 정보 (위도, 경도)
     */
    public Mono<KakaoGeocodingResponse> geocodeAddress(String address) {
        if (address == null || address.isEmpty()) {
            return Mono.empty();
        }

        try {
            String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);
            
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/local/search/address.json")
                            .queryParam("query", encodedAddress)
                            .build())
                    .header("Authorization", "KakaoAK " + restApiKey)
                    .retrieve()
                    .bodyToMono(KakaoGeocodingResponse.class)
                    .doOnError(error -> {
                        System.err.println("카카오맵 지오코딩 API 호출 오류: " + error.getMessage());
                    });
        } catch (Exception e) {
            System.err.println("주소 인코딩 오류: " + e.getMessage());
            return Mono.empty();
        }
    }

    /**
     * 주소를 좌표로 변환 (동기 방식, 간단한 사용)
     * @param address 주소
     * @return 좌표 정보 [위도, 경도] 또는 null
     */
    public Double[] geocodeAddressSync(String address) {
        try {
            KakaoGeocodingResponse response = geocodeAddress(address).block();
            
            if (response != null && 
                response.getDocuments() != null && 
                !response.getDocuments().isEmpty()) {
                
                KakaoGeocodingResponse.Document doc = response.getDocuments().get(0);
                
                // road_address가 있으면 우선 사용, 없으면 address 사용
                String latStr = null;
                String lonStr = null;
                
                if (doc.getRoadAddress() != null) {
                    latStr = doc.getRoadAddress().getLatitude();
                    lonStr = doc.getRoadAddress().getLongitude();
                } else if (doc.getAddress() != null) {
                    latStr = doc.getAddress().getLatitude();
                    lonStr = doc.getAddress().getLongitude();
                } else {
                    latStr = doc.getLatitude();
                    lonStr = doc.getLongitude();
                }
                
                if (latStr != null && lonStr != null) {
                    try {
                        double latitude = Double.parseDouble(latStr);
                        double longitude = Double.parseDouble(lonStr);
                        return new Double[]{latitude, longitude};
                    } catch (NumberFormatException e) {
                        System.err.println("좌표 파싱 오류: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("지오코딩 오류: " + e.getMessage());
        }
        
        return null;
    }
}

