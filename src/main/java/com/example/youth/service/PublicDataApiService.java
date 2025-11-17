package com.example.youth.service;

import com.example.youth.dto.publicdata.LHRentalHouseListResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class PublicDataApiService {

    private final WebClient lhWebClient;

    @Value("${public-data.lh.rental-house-list.service-key}")
    private String serviceKey;

    @Value("${public-data.lh.rental-house-list.encoding-key:}")
    private String encodingKey;

    @Value("${public-data.lh.rental-house-list.decoding-key:}")
    private String decodingKey;

    public PublicDataApiService(@Qualifier("lhWebClient") WebClient lhWebClient) {
        this.lhWebClient = lhWebClient;
    }

    /**
     * LH 공공임대주택 단지정보 조회
     * @param pageNo 페이지 번호 (기본값: 1)
     * @param numOfRows 페이지당 데이터 개수 (기본값: 10)
     * @param brtcCode 광역시도 코드 (옵션)
     * @param signguCode 시군구 코드 (옵션)
     * @param hsmpSn 단지 식별자 (옵션)
     * @return LH 임대주택 목록 응답
     */
    public Mono<LHRentalHouseListResponse> getLHRentalHouseList(
            Integer pageNo,
            Integer numOfRows,
            String brtcCode,
            String signguCode,
            String hsmpSn) {

        // 인코딩된 서비스 키 사용 (필요시)
        String encodedServiceKey = encodingKey != null && !encodingKey.isEmpty() 
                ? encodingKey 
                : serviceKey;

        Map<String, String> params = new HashMap<>();
        params.put("serviceKey", encodedServiceKey);
        params.put("pageNo", String.valueOf(pageNo != null ? pageNo : 1));
        params.put("numOfRows", String.valueOf(numOfRows != null ? numOfRows : 10));
        
        if (brtcCode != null && !brtcCode.isEmpty()) {
            params.put("brtcCode", brtcCode);
        }
        if (signguCode != null && !signguCode.isEmpty()) {
            params.put("signguCode", signguCode);
        }
        if (hsmpSn != null && !hsmpSn.isEmpty()) {
            params.put("hsmpSn", hsmpSn);
        }

        // 쿼리 파라미터 생성
        StringBuilder queryString = new StringBuilder();
        params.forEach((key, value) -> {
            if (queryString.length() > 0) {
                queryString.append("&");
            }
            try {
                queryString.append(key)
                        .append("=")
                        .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
            } catch (Exception e) {
                queryString.append(key).append("=").append(value);
            }
        });

        return lhWebClient.get()
                .uri("?" + queryString.toString())
                .retrieve()
                .bodyToMono(LHRentalHouseListResponse.class)
                .doOnError(error -> {
                    System.err.println("LH API 호출 오류: " + error.getMessage());
                });
    }

    /**
     * 전체 데이터 조회 (페이징 처리)
     */
    public Mono<LHRentalHouseListResponse> getAllLHRentalHouseList(
            String brtcCode,
            String signguCode) {
        return getLHRentalHouseList(1, 1000, brtcCode, signguCode, null);
    }
}

