package com.example.youth.service;

import com.example.youth.dto.ChatResponse;
import com.example.youth.dto.UserProfileResponse;
import com.example.youth.repository.PolicyRepository;
import com.example.youth.repository.HousingComplexRepository;
import com.example.youth.repository.HousingNoticeRepository;
import com.example.youth.repository.BookmarkRepository;
import com.example.youth.repository.InterestCategoryRepository;
import com.example.youth.util.CryptoUtil;
import com.example.youth.DB.Policy;
import com.example.youth.DB.HousingComplex;
import com.example.youth.DB.HousingNotice;
import com.example.youth.DB.Bookmark;
import com.example.youth.DB.InterestCategory;
import com.example.youth.common.ContentType;
import com.example.youth.DB.ActiveStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GeminiService {

    private WebClient webClient;
    private final ObjectMapper objectMapper;
    private final ProfanityFilterService profanityFilterService;
    private final PolicyRepository policyRepository;
    private final HousingComplexRepository housingComplexRepository;
    private final HousingNoticeRepository housingNoticeRepository;
    private final UserService userService;
    private final BookmarkRepository bookmarkRepository;
    private final InterestCategoryRepository interestCategoryRepository;

    @Value("${gemini.api.key:}")
    private String encryptedApiKey;  // 암호화된 API 키 (환경 변수에서 로드)

    @Value("${ENCRYPTION_KEY:}")
    private String encryptionKey;  // 암호화 키 (환경 변수에서 로드)

    @Value("${gemini.api.model:gemini-2.5-flash}")
    private String modelName;

    @Value("${gemini.api.base-url:https://generativelanguage.googleapis.com/v1}")
    private String baseUrl;

    @Autowired
    public GeminiService(
            ProfanityFilterService profanityFilterService,
            PolicyRepository policyRepository,
            HousingComplexRepository housingComplexRepository,
            HousingNoticeRepository housingNoticeRepository,
            UserService userService,
            BookmarkRepository bookmarkRepository,
            InterestCategoryRepository interestCategoryRepository) {
        this.profanityFilterService = profanityFilterService;
        this.policyRepository = policyRepository;
        this.housingComplexRepository = housingComplexRepository;
        this.housingNoticeRepository = housingNoticeRepository;
        this.userService = userService;
        this.bookmarkRepository = bookmarkRepository;
        this.interestCategoryRepository = interestCategoryRepository;
        this.objectMapper = new ObjectMapper();
    }

    private String apiKey;  // 복호화된 API 키
    
    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        
        // API 키 복호화
        try {
            if (encryptedApiKey == null || encryptedApiKey.isEmpty()) {
                System.err.println("  ⚠️ WARNING: GEMINI_API_KEY 환경 변수가 설정되지 않았습니다!");
                System.err.println("  환경 변수 설정 방법:");
                System.err.println("    Windows: set GEMINI_API_KEY=your_encrypted_api_key_here");
                System.err.println("    Linux/Mac: export GEMINI_API_KEY=your_encrypted_api_key_here");
                this.apiKey = null;
            } else {
                // 암호화 키 확인
                if (encryptionKey == null || encryptionKey.isEmpty()) {
                    System.err.println("  ⚠️ ERROR: ENCRYPTION_KEY 환경 변수가 설정되지 않았습니다!");
                    System.err.println("  .env 파일에 ENCRYPTION_KEY를 추가하세요.");
                    System.err.println("  예: ENCRYPTION_KEY=your_encryption_key_here");
                    this.apiKey = null;
                } else {
                    // API 키 복호화
                    try {
                        this.apiKey = CryptoUtil.decrypt(encryptedApiKey, encryptionKey);
                        
                        // API 키 로드 확인 로그
                        System.out.println("Gemini API 초기화:");
                        System.out.println("  - Base URL: " + baseUrl);
                        System.out.println("  - Model: " + modelName);
                        System.out.println("  - API Key: " + apiKey.substring(0, Math.min(10, apiKey.length())) + "... (복호화 완료)");
                    } catch (Exception e) {
                        System.err.println("  ⚠️ ERROR: API 키 복호화 실패!");
                        System.err.println("  오류 메시지: " + e.getMessage());
                        System.err.println("  ENCRYPTION_KEY와 GEMINI_API_KEY를 확인하세요.");
                        this.apiKey = null;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("  ⚠️ ERROR: API 키 복호화 실패!");
            System.err.println("  오류 메시지: " + e.getMessage());
            System.err.println("  ENCRYPTION_KEY 환경 변수를 확인하세요.");
            this.apiKey = null;
        }
    }

    /**
     * Gemini API를 사용하여 챗봇 응답 생성
     * @param message 사용자 메시지
     * @param userId 사용자 ID (선택)
     * @return 챗봇 응답
     */
    public Mono<ChatResponse> generateChatResponse(String message, String userId) {
        // 1. 비속어 및 비정상 요청 필터링
        if (profanityFilterService.containsProfanity(message)) {
            return Mono.just(ChatResponse.builder()
                    .response("부적절한 언어가 포함되어 있습니다. 정중한 표현으로 다시 질문해주세요.")
                    .actionLinks(new ArrayList<>())
                    .build());
        }
        
        // 비정상 요청 필터링 (SQL 인젝션, XSS 등)
        if (profanityFilterService.isSuspiciousRequest(message)) {
            return Mono.just(ChatResponse.builder()
                    .response("잘못된 요청입니다. 정상적인 질문으로 다시 시도해주세요.")
                    .actionLinks(new ArrayList<>())
                    .build());
        }

        // 2. 시스템 프롬프트 생성 (사용자 정보 포함)
        String systemPrompt = buildSystemPrompt(userId);

        // 3. Gemini API 요청 생성 (올바른 형식)
        Map<String, Object> requestBody = new HashMap<>();
        
        // contents 배열 생성
        Map<String, Object> content = new HashMap<>();
        content.put("role", "user");
        
        List<Map<String, Object>> parts = new ArrayList<>();
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", systemPrompt + "\n\n사용자 질문: " + message);
        parts.add(textPart);
        
        content.put("parts", parts);
        requestBody.put("contents", List.of(content));

        // 4. Gemini API 호출 (올바른 엔드포인트 형식)
        // 최종 URL: https://generativelanguage.googleapis.com/v1/models/{modelName}:generateContent?key={apiKey}
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("Gemini API 키가 설정되지 않았습니다.");
            return Mono.just(ChatResponse.builder()
                    .response("Gemini API 키가 설정되지 않았습니다. 관리자에게 문의해주세요.")
                    .actionLinks(new ArrayList<>())
                    .build());
        }
        
        String url = String.format("/models/%s:generateContent?key=%s", modelName, apiKey);
        System.out.println("Gemini API 호출 URL: " + baseUrl + url.replace("?key=" + apiKey, "?key=***"));
        
        return webClient.post()
                .uri(url)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), 
                    response -> {
                        return response.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                System.err.println("=== Gemini API Error ===");
                                System.err.println("Status: " + response.statusCode());
                                System.err.println("URL: " + baseUrl + url.replace("?key=" + apiKey, "?key=***"));
                                System.err.println("Model: " + modelName);
                                System.err.println("Error Body: " + errorBody);
                                
                                String errorMessage = "Gemini API 오류가 발생했습니다.";
                                
                                // 오류 응답 본문에서 상세 메시지 파싱
                                try {
                                    JsonNode errorJson = objectMapper.readTree(errorBody);
                                    JsonNode error = errorJson.path("error");
                                    String errorMsg = error.path("message").asText("");
                                    String reason = "";
                                    if (error.path("details").isArray() && error.path("details").size() > 0) {
                                        reason = error.path("details").get(0).path("reason").asText("");
                                    }
                                    
                                    System.err.println("오류 메시지: " + errorMsg);
                                    System.err.println("오류 이유: " + reason);
                                    
                                    if (errorMsg.contains("API key expired") || reason.equals("API_KEY_INVALID")) {
                                        errorMessage = "Gemini API 키가 만료되었습니다. 새로운 API 키를 발급받아 설정해주세요.";
                                        System.err.println("⚠️ API 키 만료됨 - 새로운 API 키 필요");
                                    } else if (response.statusCode().value() == 403) {
                                        errorMessage = "Gemini API 접근이 거부되었습니다. API 키가 유효하지 않거나 권한이 없습니다. API 키를 확인해주세요.";
                                        System.err.println("403 오류: API 키가 유효하지 않거나 권한이 없습니다.");
                                    } else if (response.statusCode().value() == 400) {
                                        errorMessage = "Gemini API 요청 오류: " + (errorMsg.isEmpty() ? "요청 형식이 잘못되었습니다." : errorMsg);
                                    } else if (response.statusCode().value() == 429) {
                                        errorMessage = "Gemini API 사용량 한도를 초과했습니다. 잠시 후 다시 시도해주세요.";
                                    }
                                } catch (Exception parseEx) {
                                    // JSON 파싱 실패 시 기본 메시지 사용
                                    System.err.println("오류 응답 파싱 실패: " + parseEx.getMessage());
                                    if (response.statusCode().value() == 400) {
                                        errorMessage = "Gemini API 요청 형식이 잘못되었습니다.";
                                    } else if (response.statusCode().value() == 403) {
                                        errorMessage = "Gemini API 접근이 거부되었습니다.";
                                    } else if (response.statusCode().value() == 429) {
                                        errorMessage = "Gemini API 사용량 한도를 초과했습니다.";
                                    }
                                }
                                
                                System.err.println("API 키 확인: " + (apiKey != null && apiKey.length() > 10 
                                        ? apiKey.substring(0, 10) + "..." 
                                        : "NULL 또는 빈 문자열"));
                                
                                return Mono.error(new RuntimeException(errorMessage + " (상태 코드: " + response.statusCode() + ")"));
                            });
                    })
                .bodyToMono(String.class)
                .map(response -> {
                    System.out.println("Gemini API Response: " + response);
                    return parseGeminiResponse(response, message);
                })
                .onErrorResume(e -> {
                    // 에러 발생 시 기본 응답 반환
                    System.err.println("=== Gemini API 호출 중 오류 발생 ===");
                    System.err.println("오류 메시지: " + e.getMessage());
                    System.err.println("오류 클래스: " + e.getClass().getName());
                    if (e.getCause() != null) {
                        System.err.println("원인: " + e.getCause().getMessage());
                    }
                    e.printStackTrace();
                    
                    // API 키 상태 확인
                    if (apiKey == null || apiKey.isEmpty()) {
                        System.err.println("⚠️ API 키가 설정되지 않았습니다!");
                    } else {
                        System.err.println("API 키 상태: " + apiKey.substring(0, Math.min(10, apiKey.length())) + "... (길이: " + apiKey.length() + ")");
                    }
                    
                    String errorMessage = "죄송합니다. 일시적인 오류가 발생했습니다.";
                    String errorDetail = e.getMessage();
                    
                    if (errorDetail != null) {
                        if (errorDetail.contains("API key expired") || errorDetail.contains("API 키가 만료되었습니다")) {
                            errorMessage = "Gemini API 키가 만료되었습니다. 관리자에게 문의해주세요.";
                            System.err.println("⚠️ API 키 만료됨");
                        } else if (errorDetail.contains("403") || errorDetail.contains("API_KEY_INVALID")) {
                            errorMessage = "Gemini API 접근이 거부되었습니다. API 키가 유효하지 않습니다. 관리자에게 문의해주세요.";
                            System.err.println("⚠️ API 키 유효하지 않음");
                        } else if (errorDetail.contains("429")) {
                            errorMessage = "API 사용량 한도를 초과했습니다. 잠시 후 다시 시도해주세요.";
                        } else if (errorDetail.contains("400")) {
                            errorMessage = errorDetail; // 이미 상세한 메시지가 포함되어 있음
                        }
                    }
                    
                    return Mono.just(ChatResponse.builder()
                            .response(errorMessage)
                            .actionLinks(new ArrayList<>())
                            .build());
                });
    }

    /**
     * 시스템 프롬프트 생성 (청년정책 전문 챗봇)
     * 사용자 프로필, 관심분야, 북마크 정보를 포함하여 맞춤형 추천 제공
     */
    private String buildSystemPrompt(String userId) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("당신은 '슬기로운 청년생활' 앱의 청년정책 전문 상담 챗봇입니다. ");
        prompt.append("사용자의 질문에 친절하고 정확하게 답변해주세요. ");
        prompt.append("청년정책, 주거 지원, 취업 지원, 창업 지원 등에 대한 정보를 제공할 수 있습니다.\n\n");
        
        // 사용자 정보 가져오기
        UserProfileResponse userProfile = null;
        List<String> interests = new ArrayList<>();
        List<String> bookmarkedPolicyIds = new ArrayList<>();
        List<String> bookmarkedHousingIds = new ArrayList<>();
        
        if (userId != null && !userId.isEmpty()) {
            try {
                // 사용자 프로필 조회
                userProfile = userService.getUserProfile(userId);
                
                // 관심분야 조회
                List<InterestCategory> interestCategories = interestCategoryRepository.findByUser_UserId(userId);
                interests = interestCategories.stream()
                        .map(InterestCategory::getCategory)
                        .collect(java.util.stream.Collectors.toList());
                
                // 북마크 조회
                List<Bookmark> bookmarks = bookmarkRepository.findByUser_UserIdAndIsActiveOrderByCreatedAtDesc(
                    userId, ActiveStatus.Y);
                for (Bookmark bookmark : bookmarks) {
                    if (bookmark.getContentType() == ContentType.policy) {
                        bookmarkedPolicyIds.add(bookmark.getContentId());
                    } else if (bookmark.getContentType() == ContentType.housing) {
                        bookmarkedHousingIds.add(bookmark.getContentId());
                    }
                }
            } catch (Exception e) {
                System.err.println("사용자 정보 조회 중 오류: " + e.getMessage());
            }
        }
        
        // 사용자 프로필 정보 추가
        if (userProfile != null) {
            prompt.append("=== 사용자 정보 ===\n");
            if (userProfile.getNickname() != null) {
                prompt.append("닉네임: ").append(userProfile.getNickname()).append("\n");
            }
            if (userProfile.getAge() != null) {
                prompt.append("나이: ").append(userProfile.getAge()).append("세\n");
            }
            if (userProfile.getRegion() != null) {
                prompt.append("지역: ").append(userProfile.getRegion()).append("\n");
            }
            if (userProfile.getEducation() != null) {
                prompt.append("학력: ").append(userProfile.getEducation()).append("\n");
            }
            if (userProfile.getJobStatus() != null) {
                prompt.append("직업 상태: ").append(userProfile.getJobStatus()).append("\n");
            }
            if (!interests.isEmpty()) {
                prompt.append("관심분야: ").append(String.join(", ", interests)).append("\n");
            }
            if (!bookmarkedPolicyIds.isEmpty()) {
                prompt.append("북마크한 정책 수: ").append(bookmarkedPolicyIds.size()).append("개\n");
            }
            if (!bookmarkedHousingIds.isEmpty()) {
                prompt.append("북마크한 주택 수: ").append(bookmarkedHousingIds.size()).append("개\n");
            }
            prompt.append("\n");
        }
        
        // 사용자 관심분야와 북마크에 맞는 정책 추천
        // 마감일이 남은 정책만, 신청 링크가 있는 정책만 추천
        java.sql.Date currentDate = new java.sql.Date(System.currentTimeMillis());
        List<Policy> recommendedPolicies = new ArrayList<>();
        if (!interests.isEmpty() || !bookmarkedPolicyIds.isEmpty()) {
            // 관심분야와 일치하는 정책 찾기
            for (String interest : interests) {
                List<Policy> policies = policyRepository.findByCategoryContaining(interest);
                recommendedPolicies.addAll(policies);
            }
            
            // 북마크한 정책 추가
            for (String policyId : bookmarkedPolicyIds) {
                policyRepository.findById(policyId).ifPresent(recommendedPolicies::add);
            }
        }
        
        // 최근 활성 정책 추가 (추천 정책이 적을 경우)
        if (recommendedPolicies.size() < 10) {
            List<Policy> recentPolicies = policyRepository.findAll().stream()
                    .limit(20)
                    .filter(p -> !recommendedPolicies.contains(p))
                    .limit(10 - recommendedPolicies.size())
                    .collect(java.util.stream.Collectors.toList());
            recommendedPolicies.addAll(recentPolicies);
        }
        
        // 필터링: 마감일이 남은 정책만, 신청 링크가 있는 정책만
        final java.sql.Date finalCurrentDate = currentDate;
        final List<Policy> finalRecommendedPolicies = recommendedPolicies;
        List<Policy> filteredPolicies = finalRecommendedPolicies.stream()
                .filter(p -> {
                    // 마감일 체크: applicationEnd가 null이거나 오늘 이후인 경우
                    if (p.getApplicationEnd() != null && p.getApplicationEnd().before(finalCurrentDate)) {
                        return false;
                    }
                    // 신청 링크 체크: link1 또는 link2가 있어야 함
                    boolean hasLink = (p.getLink1() != null && !p.getLink1().isEmpty()) 
                            || (p.getLink2() != null && !p.getLink2().isEmpty());
                    return hasLink;
                })
                .collect(java.util.stream.Collectors.toList());
        
        // 사용 가능한 정책 정보
        if (!filteredPolicies.isEmpty()) {
            prompt.append("=== 추천 정책 정보 ===\n");
            for (Policy policy : filteredPolicies.stream().limit(15).collect(java.util.stream.Collectors.toList())) {
                String summary = policy.getSummary() != null && !policy.getSummary().isEmpty() 
                    ? policy.getSummary().substring(0, Math.min(100, policy.getSummary().length())) 
                    : "상세 정보는 신청 링크를 확인하세요.";
                
                prompt.append(String.format("%s\n", policy.getTitle()));
                prompt.append(String.format("%s\n", summary));
                // 신청 링크는 ActionLink에 포함되므로 프롬프트에서 제거
                prompt.append(String.format("[정책ID:%s]\n\n", policy.getPolicyId()));
            }
        }
        
        // 임대주택 정보 (사용자 지역 기반)
        // 단지정보는 housing_complex에서, 공고문과 신청링크는 housing_notice에서 가져오기
        List<HousingComplex> recommendedComplexes = new ArrayList<>();
        List<HousingNotice> recommendedNotices = new ArrayList<>();
        
        if (userProfile != null && userProfile.getRegion() != null) {
            String region = userProfile.getRegion();
            // 지역명에서 시/도 추출 (예: "서울시 강남구" -> "서울")
            String province = region.split(" ")[0].replace("시", "").replace("도", "").replace("특별시", "").replace("광역시", "");
            
            // 단지정보 조회 (지역 기반)
            recommendedComplexes = housingComplexRepository.findByRegion(province)
                    .stream()
                    .limit(10)
                    .collect(java.util.stream.Collectors.toList());
        }
        
        // 지역 기반 조회 결과가 없으면 최근 단지정보 조회
        if (recommendedComplexes.isEmpty()) {
            recommendedComplexes = housingComplexRepository.findAll().stream()
                    .limit(10)
                    .collect(java.util.stream.Collectors.toList());
        }
        
        // 각 단지에 대한 공고문 조회 (신청링크 포함)
        // 마감일이 남은 공고문만, 신청 링크가 있는 공고문만
        for (HousingComplex complex : recommendedComplexes) {
            List<HousingNotice> notices = housingNoticeRepository.findByHsmpSn(complex.getComplexId());
            if (notices.isEmpty()) {
                // 단지명으로도 조회 시도
                notices = housingNoticeRepository.findByHsmpNm(complex.getHsmpNm());
            }
            // 필터링: 마감일이 남은 공고문만, 신청 링크가 있는 공고문만
            List<HousingNotice> validNotices = notices.stream()
                    .filter(n -> {
                        // 마감일 체크: applicationEnd가 null이거나 오늘 이후인 경우
                        if (n.getApplicationEnd() != null && n.getApplicationEnd().before(currentDate)) {
                            return false;
                        }
                        // 신청 링크 체크: dtlUrl이 있어야 함
                        return n.getDtlUrl() != null && !n.getDtlUrl().isEmpty();
                    })
                    .limit(3)
                    .collect(java.util.stream.Collectors.toList());
            recommendedNotices.addAll(validNotices);
        }
        
        // 북마크한 주택 추가 (공고문만)
        for (String housingId : bookmarkedHousingIds) {
            HousingNotice notice = housingNoticeRepository.findById(housingId).orElse(null);
            if (notice != null && !recommendedNotices.contains(notice)) {
                // 마감일과 신청 링크 체크
                if ((notice.getApplicationEnd() == null || !notice.getApplicationEnd().before(currentDate))
                        && notice.getDtlUrl() != null && !notice.getDtlUrl().isEmpty()) {
                    recommendedNotices.add(notice);
                }
            }
        }
        
        // 단지정보는 프롬프트에만 포함 (ActionLink에는 포함하지 않음)
        if (!recommendedComplexes.isEmpty()) {
            prompt.append("=== 추천 임대주택 단지정보 ===\n");
            for (HousingComplex complex : recommendedComplexes.stream().limit(10).collect(java.util.stream.Collectors.toList())) {
                prompt.append(String.format("단지명: %s\n", complex.getHsmpNm() != null ? complex.getHsmpNm() : "정보 없음"));
                if (complex.getRnAdres() != null) {
                    prompt.append(String.format("주소: %s\n", complex.getRnAdres()));
                } else if (complex.getBrtcNm() != null && complex.getSignguNm() != null) {
                    prompt.append(String.format("지역: %s %s\n", complex.getBrtcNm(), complex.getSignguNm()));
                }
                if (complex.getSupplyArea() != null) {
                    // 면적에 평수 추가 (1평 = 3.3㎡)
                    double pyeong = complex.getSupplyArea() / 3.3;
                    prompt.append(String.format("면적: %.1f㎡ (%.1f평)\n", complex.getSupplyArea(), pyeong));
                }
                if (complex.getDeposit() != null && complex.getMonthlyRent() != null) {
                    prompt.append(String.format("보증금: %d만원, 월세: %d만원\n", complex.getDeposit() / 10000, complex.getMonthlyRent() / 10000));
                }
                if (complex.getTotalUnits() != null) {
                    prompt.append(String.format("세대수: %d세대\n", complex.getTotalUnits()));
                }
                if (complex.getHouseTyNm() != null) {
                    prompt.append(String.format("주택유형: %s\n", complex.getHouseTyNm()));
                }
                if (complex.getCompleteDate() != null) {
                    prompt.append(String.format("준공일: %s\n", complex.getCompleteDate()));
                }
                // 단지정보는 ActionLink에 포함하지 않으므로 [주택ID:...] 마커 제거
                prompt.append("\n");
            }
        }
        
        // 공고문만 ActionLink에 포함
        if (!recommendedNotices.isEmpty()) {
            prompt.append("=== 추천 임대주택 공고문 ===\n");
            for (HousingNotice notice : recommendedNotices.stream().limit(10).collect(java.util.stream.Collectors.toList())) {
                prompt.append(String.format("공고명: %s\n", notice.getPanNm() != null ? notice.getPanNm() : "정보 없음"));
                if (notice.getHsmpNm() != null) {
                    prompt.append(String.format("단지명: %s\n", notice.getHsmpNm()));
                }
                if (notice.getAisTpCdNm() != null) {
                    prompt.append(String.format("공고유형: %s\n", notice.getAisTpCdNm()));
                }
                if (notice.getApplicationStart() != null && notice.getApplicationEnd() != null) {
                    prompt.append(String.format("신청 기간: %s ~ %s\n", notice.getApplicationStart(), notice.getApplicationEnd()));
                }
                if (notice.getPanNtStDt() != null) {
                    prompt.append(String.format("공고 게시일: %s\n", notice.getPanNtStDt()));
                }
                if (notice.getClsgDt() != null) {
                    prompt.append(String.format("공고 마감일: %s\n", notice.getClsgDt()));
                }
                // 신청 링크는 ActionLink에 포함되므로 프롬프트에서 제거
                prompt.append(String.format("[주택ID:%s]\n\n", notice.getNoticeId()));
            }
        }
        
        // 응답 형식 지시사항
        prompt.append("=== 응답 형식 지시사항 ===\n");
        prompt.append("1. 정책을 추천할 때는 다음 형식으로 답변하세요:\n");
        prompt.append("   각 정책마다 빈 줄로 구분하여 작성하세요.\n");
        prompt.append("   - 정책 제목 (굵게 표시하지 말고 일반 텍스트로)\n");
        prompt.append("   - 간단한 요약 정보 (1-2줄)\n");
        prompt.append("   - 신청 링크: [URL을 텍스트로만 표시, 클릭 불가능하게]\n");
        prompt.append("   - 반드시 마지막에 [정책ID:정책ID값] 형식을 포함하세요 (사용자에게는 보이지 않음)\n");
        prompt.append("   예시:\n");
        prompt.append("   청년 주거 지원 정책\n");
        prompt.append("   전월세 보증금 대출을 지원합니다.\n");
        prompt.append("   신청 링크: https://example.com\n");
        prompt.append("   [정책ID:20240104005400100002]\n\n");
        prompt.append("2. 임대주택을 추천할 때는 다음 형식으로 답변하세요:\n");
        prompt.append("   각 주택마다 빈 줄로 구분하여 작성하세요.\n");
        prompt.append("   - 단지명 또는 공고명 (굵게 표시하지 말고 일반 텍스트로)\n");
        prompt.append("   - 주소 정보 (단지정보인 경우)\n");
        prompt.append("   - 면적, 보증금, 월세, 세대수, 주택유형, 준공일 등 상세 정보 (단지정보인 경우)\n");
        prompt.append("   - 공고유형, 신청 기간, 공고 게시일, 공고 마감일 등 (공고문인 경우)\n");
        prompt.append("   - 신청 링크: [URL을 텍스트로만 표시, 클릭 불가능하게] (공고문인 경우)\n");
        prompt.append("   - 반드시 마지막에 [주택ID:주택ID값] 형식을 포함하세요 (사용자에게는 보이지 않음)\n");
        prompt.append("   주의: 주택ID는 단지정보(complexId) 또는 공고문(noticeId)일 수 있습니다.\n");
        prompt.append("   예시:\n");
        prompt.append("   가평읍내 휴먼시아2단지\n");
        prompt.append("   경기도 가평군 가평읍 가화로 164\n");
        prompt.append("   면적: 59.5㎡, 보증금: 5000만원, 월세: 20만원\n");
        prompt.append("   세대수: 200세대, 주택유형: 아파트\n");
        prompt.append("   [주택ID:complex123]\n\n");
        prompt.append("3. 중요 사항:\n");
        prompt.append("   - 정책 ID나 주택 ID는 절대 사용자에게 직접 보이지 않도록 하세요.\n");
        prompt.append("   - [정책ID:...] 또는 [주택ID:...] 형식은 내부적으로만 사용되며, 최종 응답에서 제거됩니다.\n");
        prompt.append("   - \"(policy:\", \"(housing:\", \"policy:\", \"housing:\" 같은 형식은 절대 사용하지 마세요.\n");
        prompt.append("   - 신청 링크는 클릭 불가능한 일반 텍스트로만 표시하세요. (예: \"신청 링크: https://example.com\")\n");
        prompt.append("   - 각 정책/주택마다 빈 줄로 명확하게 구분하세요.\n");
        prompt.append("   - 마크다운 형식(**, *, 굵게 등)을 사용하지 마세요. 일반 텍스트로만 작성하세요.\n\n");
        prompt.append("4. 사용자의 프로필, 관심분야, 북마크를 우선적으로 고려하여 맞춤형 추천을 제공하세요.\n");
        prompt.append("5. 답변은 친절하고 간결하게 작성하세요.\n");
        prompt.append("6. 정책이나 주택을 여러 개 추천할 때는 각각에 대해 제목과 요약을 제공하세요.\n");
        
        return prompt.toString();
    }

    /**
     * Gemini API 응답 파싱
     */
    private ChatResponse parseGeminiResponse(String responseJson, String userMessage) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode candidates = root.path("candidates");
            
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode candidate = candidates.get(0);
                JsonNode content = candidate.path("content");
                JsonNode parts = content.path("parts");
                
                if (parts.isArray() && parts.size() > 0) {
                    String text = parts.get(0).path("text").asText();
                    
                    // 응답에서 액션 링크 추출 (정책 ID, 주택 ID 등)
                    List<ChatResponse.ActionLink> actionLinks = extractActionLinks(text, userMessage);
                    
                    // 응답 텍스트 정리
                    String cleanedText = text;
                    
                    // policy ID와 주택 ID 마커 제거 (사용자에게 보이지 않도록)
                    cleanedText = cleanedText.replaceAll("\\[정책ID:[^\\]]+\\]", "");
                    cleanedText = cleanedText.replaceAll("\\[주택ID:[^\\]]+\\]", "");
                    
                    // 불필요한 형식 제거 (policy:, housing: 등)
                    cleanedText = cleanedText.replaceAll("\\(policy:[^\\)]+\\)", "");
                    cleanedText = cleanedText.replaceAll("\\(housing:[^\\)]+\\)", "");
                    cleanedText = cleanedText.replaceAll("(?i)policy\\s*:", "");
                    cleanedText = cleanedText.replaceAll("(?i)housing\\s*:", "");
                    
                    // 마크다운 형식 제거 (굵게 표시 등)
                    cleanedText = cleanedText.replaceAll("\\*\\*([^*]+)\\*\\*", "$1"); // **텍스트** -> 텍스트
                    cleanedText = cleanedText.replaceAll("\\*([^*]+)\\*", "$1"); // *텍스트* -> 텍스트
                    
                    // 문단 구분 개선
                    // 각 정책/주택 항목 앞에 빈 줄 추가 (리스트 항목 구분)
                    cleanedText = cleanedText.replaceAll("(\\n|^)(\\*\\s+)", "$1\n$2"); // 리스트 항목 앞에 빈 줄
                    
                    // 신청 링크 텍스트 제거 (ActionLink 버튼에 있으므로)
                    cleanedText = cleanedText.replaceAll("신청 링크:.*\\n", "");
                    
                    // 연속된 공백 정리 (단, 문단 구분은 유지)
                    cleanedText = cleanedText.replaceAll("([^\\n])\\s{2,}([^\\n])", "$1 $2"); // 같은 줄 내 연속 공백
                    cleanedText = cleanedText.replaceAll("\\n{3,}", "\n\n"); // 3개 이상의 연속 줄바꿈을 2개로
                    cleanedText = cleanedText.trim();
                    
                    return ChatResponse.builder()
                            .response(cleanedText)
                            .actionLinks(actionLinks)
                            .build();
                }
            }
            
            return ChatResponse.builder()
                    .response("응답을 생성할 수 없습니다. 다시 질문해주세요.")
                    .actionLinks(new ArrayList<>())
                    .build();
                    
        } catch (Exception e) {
            return ChatResponse.builder()
                    .response("응답 처리 중 오류가 발생했습니다.")
                    .actionLinks(new ArrayList<>())
                    .build();
        }
    }

    /**
     * 응답 텍스트에서 액션 링크 추출 (정책 ID, 주택 ID 등)
     * 패턴: [정책ID:policy123] 또는 [주택ID:housing456]
     * 응답 텍스트에서 policy ID는 제거하고 사용자에게는 보이지 않도록 처리
     */
    private List<ChatResponse.ActionLink> extractActionLinks(String response, String userMessage) {
        List<ChatResponse.ActionLink> links = new ArrayList<>();
        
        // 정책 ID 패턴: [정책ID:policy123] 또는 [정책ID:policy-123]
        Pattern policyPattern = Pattern.compile("\\[정책ID:([^\\]]+)\\]", Pattern.CASE_INSENSITIVE);
        Matcher policyMatcher = policyPattern.matcher(response);
        
        while (policyMatcher.find()) {
            String policyId = policyMatcher.group(1).trim();
            Policy policy = policyRepository.findById(policyId).orElse(null);
            
            if (policy != null) {
                // URL 우선순위: link1 > link2
                String url = (policy.getLink1() != null && !policy.getLink1().isEmpty()) 
                    ? policy.getLink1() 
                    : (policy.getLink2() != null ? policy.getLink2() : "");
                
                // 신청 링크가 없으면 ActionLink에 포함하지 않음
                if (url.isEmpty()) {
                    continue;
                }
                
                // 마감일 체크
                java.sql.Date currentDate = new java.sql.Date(System.currentTimeMillis());
                if (policy.getApplicationEnd() != null && policy.getApplicationEnd().before(currentDate)) {
                    continue;
                }
                
                // 요약 정보 (100자 제한)
                String summary = policy.getSummary() != null && !policy.getSummary().isEmpty()
                    ? policy.getSummary().substring(0, Math.min(100, policy.getSummary().length()))
                    : "상세 정보는 신청 링크를 확인하세요.";
                
                links.add(ChatResponse.ActionLink.builder()
                        .type("policy")
                        .id(policyId)
                        .title(policy.getTitle())
                        .summary(summary)
                        .url(url)
                        .build());
            }
        }
        
        // 주택 ID 패턴: [주택ID:housing123] 또는 [주택ID:housing-123]
        Pattern housingPattern = Pattern.compile("\\[주택ID:([^\\]]+)\\]", Pattern.CASE_INSENSITIVE);
        Matcher housingMatcher = housingPattern.matcher(response);
        
        while (housingMatcher.find()) {
            String housingId = housingMatcher.group(1).trim();
            
            // 단지정보(complex)는 ActionLink에 포함하지 않음
            // 공고문(notice)만 ActionLink에 포함
            HousingNotice notice = housingNoticeRepository.findById(housingId).orElse(null);
            if (notice != null) {
                // 마감일과 신청 링크 체크
                java.sql.Date currentDate = new java.sql.Date(System.currentTimeMillis());
                if ((notice.getApplicationEnd() == null || !notice.getApplicationEnd().before(currentDate))
                        && notice.getDtlUrl() != null && !notice.getDtlUrl().isEmpty()) {
                    
                    String summary = notice.getHsmpNm() != null 
                        ? notice.getHsmpNm() 
                        : "상세 정보는 링크를 확인하세요.";
                    
                    if (notice.getAisTpCdNm() != null) {
                        summary = notice.getAisTpCdNm() + (summary.isEmpty() ? "" : " - " + summary);
                    }
                    
                    String url = notice.getDtlUrl();
                    
                    links.add(ChatResponse.ActionLink.builder()
                            .type("housing")
                            .id(housingId)
                            .title(notice.getPanNm() != null ? notice.getPanNm() : "임대주택 공고")
                            .summary(summary)
                            .url(url)
                            .build());
                }
            }
        }
        
        return links;
    }
}

