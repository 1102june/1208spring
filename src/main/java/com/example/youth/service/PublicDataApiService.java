package com.example.youth.service;

import com.example.youth.dto.publicdata.LHRentalHouseListResponse;
import com.example.youth.dto.publicdata.LHRentalNoticeResponse;
import com.example.youth.dto.publicdata.YouthPolicyResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class PublicDataApiService {

    private final WebClient lhWebClient;
    private final WebClient lhRentalNoticeWebClient;
    private final WebClient youthPolicyWebClient;

    @Value("${public-data.lh.rental-house-list.service-key}")
    private String serviceKey;

    @Value("${public-data.lh.rental-house-list.encoding-key:}")
    private String encodingKey;

    @Value("${public-data.lh.rental-house-list.decoding-key:}")
    private String decodingKey;

    @Value("${public-data.lh.rental-notice.decoding-key:}")
    private String rentalNoticeDecodingKey;

    @Value("${public-data.lh.rental-notice.encoding-key:}")
    private String rentalNoticeEncodingKey;

    @Value("${public-data.lh.rental-house-list.url}")
    private String lhRentalHouseListUrl;

    @Value("${public-data.lh.rental-notice.url}")
    private String lhRentalNoticeUrl; // Added for debugging

    @Value("${youth-policy.service-key}")
    private String youthPolicyServiceKey;

    @Value("${youth-policy.url}")
    private String youthPolicyUrl;

    public PublicDataApiService(
            @Qualifier("lhWebClient") WebClient lhWebClient,
            @Qualifier("lhRentalNoticeWebClient") WebClient lhRentalNoticeWebClient,
            @Qualifier("youthPolicyWebClient") WebClient youthPolicyWebClient) {
        this.lhWebClient = lhWebClient;
        this.lhRentalNoticeWebClient = lhRentalNoticeWebClient;
        this.youthPolicyWebClient = youthPolicyWebClient;
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

        // 서비스 키 결정: data.myhome.go.kr API
        // 반드시 디코딩 키를 URL 인코딩해서 사용해야 함
        String rawServiceKey;
        String encodedServiceKey;
        
        // 1순위: 디코딩 키를 URL 인코딩
        if (decodingKey != null && !decodingKey.isEmpty()) {
            rawServiceKey = decodingKey;
            try {
                encodedServiceKey = URLEncoder.encode(decodingKey, StandardCharsets.UTF_8);
            } catch (Exception e) {
                encodedServiceKey = decodingKey;
            }
        }
        // 2순위: 인코딩 키 사용 (이미 URL 인코딩된 키)
        else if (encodingKey != null && !encodingKey.isEmpty()) {
            rawServiceKey = encodingKey;
            encodedServiceKey = encodingKey;
        }
        // 3순위: service-key 사용
        else {
            rawServiceKey = serviceKey;
            try {
                encodedServiceKey = URLEncoder.encode(serviceKey, StandardCharsets.UTF_8);
            } catch (Exception e) {
                encodedServiceKey = serviceKey;
            }
        }

        // 서비스 키 로그는 제거 (전체 조회 시 로그가 너무 많아짐)

        // serviceKey는 이미 한 번 인코딩된 값을 사용
        // 전체 URL 문자열을 직접 구성하여 URI.create()로 전달 (이중 인코딩 방지)
        final String finalEncodedServiceKeyForUri = encodedServiceKey;
        
        // 전체 URL 문자열 구성 (이미 인코딩된 serviceKey를 그대로 사용)
        // API 문서 참고: https://www.data.go.kr/data/15058476/openapi.do
        // 필수 파라미터: serviceKey, brtcCode, signguCode
        // 옵션 파라미터: pageNo (기본값:1), numOfRows (기본값:10)
        // 요청주소: https://data.myhome.go.kr:443/rentalHouseList
        
        // brtcCode와 signguCode는 필수 파라미터
        // 하지만 hsmpSn만으로도 조회가 가능할 수 있으므로, hsmpSn이 있으면 brtcCode와 signguCode는 기본값 사용
        if ((brtcCode == null || brtcCode.isEmpty()) && (hsmpSn == null || hsmpSn.isEmpty())) {
            throw new IllegalArgumentException("brtcCode (광역시도 코드)는 필수 파라미터입니다. (hsmpSn이 없을 경우)");
        }
        if ((signguCode == null || signguCode.isEmpty()) && (hsmpSn == null || hsmpSn.isEmpty())) {
            throw new IllegalArgumentException("signguCode (시군구 코드)는 필수 파라미터입니다. (hsmpSn이 없을 경우)");
        }
        
        // hsmpSn만 있고 brtcCode/signguCode가 없으면 기본값 사용 (00, 000)
        String finalBrtcCode = (brtcCode != null && !brtcCode.isEmpty()) ? brtcCode : "00";
        String finalSignguCode = (signguCode != null && !signguCode.isEmpty()) ? signguCode : "000";
        
        StringBuilder urlString = new StringBuilder(lhRentalHouseListUrl);
        urlString.append("?serviceKey=").append(finalEncodedServiceKeyForUri) // 필수: 서비스키
                 .append("&brtcCode=").append(URLEncoder.encode(finalBrtcCode, StandardCharsets.UTF_8)) // 필수: 광역시도 코드
                 .append("&signguCode=").append(URLEncoder.encode(finalSignguCode, StandardCharsets.UTF_8)) // 필수: 시군구 코드
                 .append("&pageNo=").append(pageNo != null ? pageNo : 1) // 옵션: 페이지 번호 (기본값:1)
                 .append("&numOfRows=").append(numOfRows != null ? numOfRows : 10); // 옵션: 페이지당 데이터 개수 (기본값:10)
        if (hsmpSn != null && !hsmpSn.isEmpty()) {
            urlString.append("&hsmpSn=").append(URLEncoder.encode(hsmpSn, StandardCharsets.UTF_8));
        }
        
        String fullUrl = urlString.toString();
        // URL 로그는 제거 (전체 조회 시 로그가 너무 많아짐)
        
        // URI 객체를 직접 생성하여 WebClient에 전달 (이중 인코딩 방지)
        // baseUrl이 설정된 WebClient와 충돌을 피하기 위해 WebClient.create() 사용
        URI uri = URI.create(fullUrl);
        
        return WebClient.create()
                .get()
                .uri(uri) // URI 객체를 직접 전달 (WebClient가 추가 인코딩하지 않음)
                .accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML)
                .exchangeToMono(response -> {
                    // HTTP 상태 코드 확인
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(String.class)
                                .doOnNext(responseBody -> {
                                    // 응답 내용 확인 (디버깅용) - HTML 에러만 로그 출력
                                    if (responseBody != null) {
                                        // HTML 응답인지 확인
                                        if (responseBody.trim().startsWith("<") || responseBody.contains("<html")) {
                                            System.err.println("⚠️ HTML 응답이 반환되었습니다 (API 에러 가능성):");
                                            System.err.println("응답 내용 (처음 1000자): " + 
                                                (responseBody.length() > 1000 ? responseBody.substring(0, 1000) : responseBody));
                                        }
                                        // 정상 JSON 응답은 로그 출력하지 않음 (전체 조회 시 로그가 너무 많아짐)
                                    }
                                })
                                .map(responseBody -> {
                                    // HTML 응답인 경우 에러 처리
                                    if (responseBody != null && (responseBody.trim().startsWith("<") || responseBody.contains("<html"))) {
                                        System.err.println("API가 HTML 에러 페이지를 반환했습니다.");
                                        throw new RuntimeException("API가 HTML 에러 페이지를 반환했습니다. 응답: " + 
                                            (responseBody.length() > 500 ? responseBody.substring(0, 500) : responseBody));
                                    }
                                    
                                    // JSON 파싱 시도
                                    try {
                                        // 응답이 비어있는지 확인
                                        if (responseBody == null || responseBody.trim().isEmpty()) {
                                            System.err.println("⚠️ 단지정보 API 응답이 비어있습니다.");
                                            return new LHRentalHouseListResponse();
                                        }
                                        
                                        // 응답 길이 제한 확인 (너무 긴 응답은 문제일 수 있음)
                                        if (responseBody.length() > 10_000_000) { // 10MB 제한
                                            System.err.println("⚠️ 단지정보 API 응답이 너무 큽니다: " + responseBody.length() + " bytes");
                                            System.err.println("⚠️ 응답 처음 1000자: " + responseBody.substring(0, 1000));
                                        }
                                        
                                        // JSON 응답에서 빈 객체 {}를 null로 변환 (competDe, heatMthdDetailNm 등)
                                        // LH API가 일부 필드를 빈 객체로 반환하는 문제 해결
                                        String cleanedResponseBody = preprocessJsonResponse(responseBody);
                                        
                                        ObjectMapper objectMapper = new ObjectMapper();
                                        // FAIL_ON_UNKNOWN_PROPERTIES를 false로 설정 (이미 Item에 @JsonIgnoreProperties 있음)
                                        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                                        // FAIL_ON_INVALID_SUBTYPE도 false로 설정
                                        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);
                                        // 빈 객체를 null로 처리하도록 설정
                                        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
                                        
                                        LHRentalHouseListResponse result = objectMapper.readValue(cleanedResponseBody, LHRentalHouseListResponse.class);
                                        
                                        // 응답 코드 확인 (새 형식: code, 기존 형식: response.header.resultCode)
                                        String resultCode = null;
                                        String resultMsg = null;
                                        
                                        // 새 형식 확인: {"code":"000","hsmpList":[],"msg":"OK"}
                                        if (result != null && result.getCode() != null) {
                                            resultCode = result.getCode();
                                            resultMsg = result.getMsg();
                                        }
                                        // 기존 형식 확인: response.header
                                        else if (result != null && result.getResponse() != null 
                                                && result.getResponse().getHeader() != null) {
                                            resultCode = result.getResponse().getHeader().getResultCode();
                                            resultMsg = result.getResponse().getHeader().getResultMsg();
                                        }
                                        
                                        // 에러 코드 체크
                                        if (resultCode != null && !resultCode.equals("000") && !resultCode.equals("00")) {
                                            System.err.println("⚠️ 단지정보 API 응답 에러 코드: " + resultCode);
                                            System.err.println("⚠️ 단지정보 API 응답 에러 메시지: " + resultMsg);
                                            // 에러 응답이면 예외 발생
                                            throw new RuntimeException("단지정보 API 에러: " + resultCode + " - " + resultMsg);
                                        }
                                        
                                        // 단지정보가 실제로 있는지 확인 (getItems() 메서드 사용)
                                        List<LHRentalHouseListResponse.Item> items = result != null ? result.getItems() : null;
                                        // 데이터가 있을 때만 로그 출력 (전체 조회 시 로그가 너무 많아짐)
                                        // 데이터가 없는 것은 정상일 수 있음 (해당 지역에 데이터가 없을 수 있음)
                                        
                                        return result;
                                    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                                        // JSON 파싱 오류 상세 정보 출력
                                        System.err.println("❌ JSON 파싱 오류: " + e.getMessage());
                                        System.err.println("❌ 오류 위치: " + e.getLocation());
                                        System.err.println("❌ 응답 처음 2000자: " + 
                                            (responseBody != null && responseBody.length() > 2000 
                                                ? responseBody.substring(0, 2000) 
                                                : responseBody));
                                        // JSON 형식 검증을 위해 전체 응답도 출력 (길이 제한)
                                        if (responseBody != null && responseBody.length() < 10000) {
                                            System.err.println("❌ 전체 응답 본문: " + responseBody);
                                        } else if (responseBody != null) {
                                            System.err.println("❌ 응답 끝부분 2000자: " + responseBody.substring(Math.max(0, responseBody.length() - 2000)));
                                        }
                                        // 빈 응답 반환하여 처리 계속
                                        System.err.println("⚠️ JSON 파싱 실패로 인해 빈 응답을 반환합니다.");
                                        return new LHRentalHouseListResponse();
                                    } catch (Exception e) {
                                        System.err.println("❌ JSON 파싱 실패 (기타 오류): " + e.getClass().getSimpleName() + " - " + e.getMessage());
                                        if (e.getCause() != null) {
                                            System.err.println("❌ 원인: " + e.getCause().getClass().getSimpleName() + " - " + e.getCause().getMessage());
                                        }
                                        System.err.println("❌ 응답 처음 2000자: " + 
                                            (responseBody != null && responseBody.length() > 2000 
                                                ? responseBody.substring(0, 2000) 
                                                : responseBody));
                                        // 빈 응답 반환하여 처리 계속
                                        System.err.println("⚠️ 오류로 인해 빈 응답을 반환합니다.");
                                        return new LHRentalHouseListResponse();
                                    }
                                });
                    } else {
                        // 에러 응답도 본문 읽기
                        return response.bodyToMono(String.class)
                                .doOnNext(errorBody -> {
                                    System.err.println("LH API 에러 응답 (상태코드: " + response.statusCode() + "): " + 
                                        (errorBody != null && errorBody.length() > 1000 
                                            ? errorBody.substring(0, 1000) 
                                            : errorBody));
                                })
                                .flatMap(errorBody -> {
                                    return Mono.error(new RuntimeException("LH API 호출 실패: " + response.statusCode() + 
                                        (errorBody != null ? " - " + errorBody : "")));
                                });
                    }
                })
                .doOnError(error -> {
                    // 에러 로그는 실제 에러 발생 시에만 출력 (빈 응답은 정상일 수 있음)
                    // System.err.println("LH API 호출 오류: " + error.getMessage());
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


    /**
     * JSON 응답에서 빈 객체 {}를 null로 변환하여 파싱 오류 방지
     * LH API가 일부 필드를 빈 객체로 반환하는 문제 해결
     */
    private String preprocessJsonResponse(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.isEmpty()) {
            return jsonResponse;
        }
        
        // 빈 객체 {}를 null로 변환 (String 필드에 대해서만)
        // 패턴: "fieldName":{}  -> "fieldName":null
        // 주의: 숫자나 다른 값에 영향을 주지 않도록 주의
        String processed = jsonResponse;
        
        // 일반적인 String 필드들이 빈 객체로 오는 경우 처리
        // 정규표현식으로 "필드명":{} 패턴을 "필드명":null로 변경
        // 주의: 값의 컨텍스트를 확인하여 안전하게 처리
        processed = processed.replaceAll("\"competDe\":\\s*\\{\\s*\\}", "\"competDe\":null");
        processed = processed.replaceAll("\"heatMthdDetailNm\":\\s*\\{\\s*\\}", "\"heatMthdDetailNm\":null");
        processed = processed.replaceAll("\"buldStleNm\":\\s*\\{\\s*\\}", "\"buldStleNm\":null");
        processed = processed.replaceAll("\"elvtrInstlAtNm\":\\s*\\{\\s*\\}", "\"elvtrInstlAtNm\":null");
        processed = processed.replaceAll("\"houseTyNm\":\\s*\\{\\s*\\}", "\"houseTyNm\":null");
        processed = processed.replaceAll("\"styleNm\":\\s*\\{\\s*\\}", "\"styleNm\":null");
        processed = processed.replaceAll("\"bassCnvrsGtnLmt\":\\s*\\{\\s*\\}", "\"bassCnvrsGtnLmt\":null");
        
        return processed;
    }

    /**
     * LH 분양임대공고문 조회
     * API 문서: https://www.data.go.kr/data/15058530/openapi.do
     * 필수 파라미터: ServiceKey, PG_SZ, PAGE, PAN_NT_ST_DT, CLSG_DT
     * @param pageNo 페이지 번호 (기본값: 1) -> PAGE 파라미터로 변환
     * @param numOfRows 페이지당 데이터 개수 (기본값: 10) -> PG_SZ 파라미터로 변환
     * @param brtcCode 광역시도 코드 (옵션) -> CNP_CD 파라미터로 변환
     * @param signguCode 시군구 코드 (옵션) - 이 API에서는 사용 안 함
     * @param hsmpSn 단지 식별자 (옵션) - 이 API에서는 사용 안 함
     * @return LH 분양임대공고문 응답
     */
    public Mono<LHRentalNoticeResponse> getLHRentalNoticeList(
            Integer pageNo,
            Integer numOfRows,
            String brtcCode,
            String signguCode,
            String hsmpSn) {

        // 서비스 키 결정 및 인코딩 (rental-notice용)
        // 반드시 디코딩 키를 URL 인코딩해서 사용해야 함
        String rawServiceKey;
        String encodedServiceKey;
        
        // 1순위: 디코딩 키를 URL 인코딩
        if (rentalNoticeDecodingKey != null && !rentalNoticeDecodingKey.isEmpty()) {
            rawServiceKey = rentalNoticeDecodingKey;
            try {
                encodedServiceKey = URLEncoder.encode(rentalNoticeDecodingKey, StandardCharsets.UTF_8);
            } catch (Exception e) {
                encodedServiceKey = rentalNoticeDecodingKey;
            }
        }
        // 2순위: 기본 디코딩 키를 URL 인코딩
        else if (decodingKey != null && !decodingKey.isEmpty()) {
            rawServiceKey = decodingKey;
            try {
                encodedServiceKey = URLEncoder.encode(decodingKey, StandardCharsets.UTF_8);
            } catch (Exception e) {
                encodedServiceKey = decodingKey;
            }
        }
        // 3순위: 인코딩 키 사용 (이미 URL 인코딩된 키)
        else if (rentalNoticeEncodingKey != null && !rentalNoticeEncodingKey.isEmpty()) {
            rawServiceKey = rentalNoticeEncodingKey;
            encodedServiceKey = rentalNoticeEncodingKey;
        }
        // 4순위: service-key 사용
        else {
            rawServiceKey = serviceKey;
            try {
                encodedServiceKey = URLEncoder.encode(serviceKey, StandardCharsets.UTF_8);
            } catch (Exception e) {
                encodedServiceKey = serviceKey;
            }
        }
        
        // final 변수로 복사하여 람다에서 사용
        final String finalEncodedServiceKey = encodedServiceKey;

        // 필수 파라미터: 공고게시일, 공고마감일
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate startDate = today.minusYears(10); // 10년 전부터
        java.time.LocalDate endDate = today.plusYears(1); // 1년 후까지
        
        String panNtStDt = startDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        String clsgDt = endDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        
        System.out.println("공고문 API 서비스 키 (원본, 처음 50자): " + (rawServiceKey != null && rawServiceKey.length() > 50 
                ? rawServiceKey.substring(0, 50) + "..." 
                : rawServiceKey));
        System.out.println("공고문 API 서비스 키 (인코딩됨, 처음 50자): " + (encodedServiceKey != null && encodedServiceKey.length() > 50 
                ? encodedServiceKey.substring(0, 50) + "..." 
                : encodedServiceKey));

        // 필수 파라미터: 공고게시일, 공고마감일
        // API 문서: https://www.data.go.kr/data/15058530/openapi.do
        // 요청주소: http://apis.data.go.kr/B552555/lhLeaseNoticeInfo1/lhLeaseNoticeInfo1
        System.out.println("LH 공고문 API 요청 준비:");
        System.out.println("  - Base URL: " + lhRentalNoticeUrl);
        System.out.println("  - Path: /lhLeaseNoticeInfo1/lhLeaseNoticeInfo1");
        System.out.println("  - PAN_NT_ST_DT: " + panNtStDt);
        System.out.println("  - CLSG_DT: " + clsgDt);

        // serviceKey는 이미 한 번 인코딩된 값을 사용
        // 전체 URL 문자열을 직접 구성하여 URI.create()로 전달 (이중 인코딩 방지)
        final String finalEncodedServiceKeyForUri = finalEncodedServiceKey;
        
        // 전체 URL 문자열 구성 (이미 인코딩된 serviceKey를 그대로 사용)
        // API 문서에 따르면 요청주소는 /lhLeaseNoticeInfo1/lhLeaseNoticeInfo1
        StringBuilder urlString = new StringBuilder(lhRentalNoticeUrl);
        urlString.append("/lhLeaseNoticeInfo1/lhLeaseNoticeInfo1")
                 .append("?serviceKey=").append(finalEncodedServiceKeyForUri) // 이미 인코딩된 키를 그대로 사용
                 .append("&PG_SZ=").append(numOfRows != null ? numOfRows : 10)
                 .append("&PAGE=").append(pageNo != null ? pageNo : 1)
                 .append("&PAN_NT_ST_DT=").append(panNtStDt)
                 .append("&CLSG_DT=").append(clsgDt);
        
        if (brtcCode != null && !brtcCode.isEmpty()) {
            // brtcCode는 인코딩 필요
            urlString.append("&CNP_CD=").append(URLEncoder.encode(brtcCode, StandardCharsets.UTF_8));
        }
        
        String fullUrl = urlString.toString();
        System.out.println("공고문 API 최종 요청 URL: " + fullUrl);
        
        // URI 객체를 직접 생성하여 WebClient에 전달 (이중 인코딩 방지)
        // baseUrl이 설정된 WebClient와 충돌을 피하기 위해 WebClient.create() 사용
        URI uri = URI.create(fullUrl);
        
        return WebClient.create()
                .get()
                .uri(uri) // URI 객체를 직접 전달 (WebClient가 추가 인코딩하지 않음)
                .accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML)
                .exchangeToMono(response -> {
                    // HTTP 상태 코드 확인
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(String.class)
                                .doOnNext(responseBody -> {
                                    // 응답 내용 확인 (디버깅용)
                                    if (responseBody != null && responseBody.length() < 500) {
                                        System.out.println("공고문 API 응답 (처음 500자): " + responseBody);
                                    } else if (responseBody != null) {
                                        System.out.println("공고문 API 응답 (처음 500자): " + responseBody.substring(0, 500));
                                    }
                                })
                                .map(responseBody -> {
                                    // JSON 파싱 시도
                                    // 실제 API 응답은 배열로 시작: [{"dsSch":[...]}, {"dsList":[...]}]
                                    try {
                                        // 응답이 비어있는지 확인
                                        if (responseBody == null || responseBody.trim().isEmpty()) {
                                            System.err.println("⚠️ 공고문 API 응답이 비어있습니다.");
                                            return new LHRentalNoticeResponse();
                                        }
                                        
                                        ObjectMapper objectMapper = new ObjectMapper();
                                        // FAIL_ON_UNKNOWN_PROPERTIES를 false로 설정
                                        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                                        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);
                                        
                                        // 배열로 파싱
                                        List<Object> responseArray = objectMapper.readValue(responseBody, List.class);
                                        
                                        // 두 번째 요소에서 dsList 추출
                                        if (responseArray != null && responseArray.size() >= 2) {
                                            Object secondElement = responseArray.get(1);
                                            String secondElementJson = objectMapper.writeValueAsString(secondElement);
                                            return objectMapper.readValue(secondElementJson, LHRentalNoticeResponse.class);
                                        } else {
                                            // 배열이 아니거나 요소가 부족한 경우 빈 응답 반환
                                            System.err.println("⚠️ 공고문 API 응답 배열 형식이 예상과 다릅니다. 배열 크기: " + 
                                                (responseArray != null ? responseArray.size() : 0));
                                            return new LHRentalNoticeResponse();
                                        }
                                    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                                        // JSON 파싱 오류 상세 정보 출력
                                        System.err.println("❌ 공고문 API JSON 파싱 오류: " + e.getMessage());
                                        System.err.println("❌ 오류 위치: " + e.getLocation());
                                        System.err.println("❌ 응답 처음 2000자: " + 
                                            (responseBody != null && responseBody.length() > 2000 
                                                ? responseBody.substring(0, 2000) 
                                                : responseBody));
                                        // 빈 응답 반환하여 처리 계속
                                        System.err.println("⚠️ JSON 파싱 실패로 인해 빈 응답을 반환합니다.");
                                        return new LHRentalNoticeResponse();
                                    } catch (Exception e) {
                                        System.err.println("❌ 공고문 API JSON 파싱 실패 (기타 오류): " + e.getClass().getSimpleName() + " - " + e.getMessage());
                                        if (e.getCause() != null) {
                                            System.err.println("❌ 원인: " + e.getCause().getClass().getSimpleName() + " - " + e.getCause().getMessage());
                                        }
                                        System.err.println("❌ 응답 처음 2000자: " + 
                                            (responseBody != null && responseBody.length() > 2000 
                                                ? responseBody.substring(0, 2000) 
                                                : responseBody));
                                        // 빈 응답 반환하여 처리 계속
                                        System.err.println("⚠️ 오류로 인해 빈 응답을 반환합니다.");
                                        return new LHRentalNoticeResponse();
                                    }
                                });
                    } else {
                        // 에러 응답도 본문 읽기
                        return response.bodyToMono(String.class)
                                .doOnNext(errorBody -> {
                                    System.err.println("공고문 API 에러 응답 (상태코드: " + response.statusCode() + "): " + 
                                        (errorBody != null && errorBody.length() > 500 
                                            ? errorBody.substring(0, 500) 
                                            : errorBody));
                                })
                                .flatMap(errorBody -> {
                                    return Mono.error(new RuntimeException("공고문 API 호출 실패: " + response.statusCode() + 
                                        (errorBody != null ? " - " + errorBody : "")));
                                });
                    }
                })
                .doOnError(error -> {
                    System.err.println("LH 공고문 API 호출 오류: " + error.getMessage());
                });
    }

    /**
     * 전체 공고문 데이터 조회 (페이징 처리)
     */
    public Mono<LHRentalNoticeResponse> getAllLHRentalNoticeList(
            String brtcCode,
            String signguCode) {
        return getLHRentalNoticeList(1, 1000, brtcCode, signguCode, null);
    }

    /**
     * 온통청년 청년정책 조회
     * 파이썬 코드 참고: apiKeyNm, rtnType=json, pageNum, pageSize
     * @param pageNum 페이지 번호 (기본값: 1)
     * @param pageSize 페이지당 데이터 개수 (기본값: 100)
     * @return 청년정책 응답
     */
    public Mono<YouthPolicyResponse> getYouthPolicyList(
            Integer pageNum,
            Integer pageSize) {

        System.out.println("청년정책 API 서비스 키: " + (youthPolicyServiceKey != null && youthPolicyServiceKey.length() > 20
                ? youthPolicyServiceKey.substring(0, 20) + "..."
                : youthPolicyServiceKey));

        // URI 빌더 생성 (파이썬 코드 참고)
        StringBuilder uriBuilder = new StringBuilder();
        uriBuilder.append("?apiKeyNm=").append(URLEncoder.encode(youthPolicyServiceKey, StandardCharsets.UTF_8))
                .append("&rtnType=json")
                .append("&pageNum=").append(pageNum != null ? pageNum : 1)
                .append("&pageSize=").append(pageSize != null ? pageSize : 100);

        String requestUri = uriBuilder.toString();
        System.out.println("청년정책 API 요청 URL: " + youthPolicyUrl + requestUri);

        return youthPolicyWebClient.get()
                .uri(requestUri)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(responseBody -> {
                    // 응답 내용 확인 (디버깅용)
                    if (responseBody != null && responseBody.length() < 2000) {
                        System.out.println("청년정책 API 응답 (전체): " + responseBody);
                    } else if (responseBody != null) {
                        System.out.println("청년정책 API 응답 (처음 2000자): " + responseBody.substring(0, 2000));
                    }
                })
                .map(responseBody -> {
                    // JSON 파싱 시도
                    try {
                        ObjectMapper objectMapper = new ObjectMapper();
                        return objectMapper.readValue(responseBody, YouthPolicyResponse.class);
                    } catch (Exception e) {
                        System.err.println("청년정책 API JSON 파싱 실패. 응답 내용: " +
                                (responseBody != null && responseBody.length() > 1000
                                        ? responseBody.substring(0, 1000)
                                        : responseBody));
                        throw new RuntimeException("청년정책 API 응답 파싱 실패: " + e.getMessage(), e);
                    }
                })
                .doOnError(error -> {
                    System.err.println("청년정책 API 호출 오류: " + error.getMessage());
                });
    }

    /**
     * 전체 청년정책 데이터 조회 (페이징 처리)
     */
    public Mono<YouthPolicyResponse> getAllYouthPolicyList() {
        return getYouthPolicyList(1, 100);
    }
}

