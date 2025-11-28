package com.example.youth.service;

import com.example.youth.dto.ChatResponse;
import com.example.youth.repository.PolicyRepository;
import com.example.youth.repository.HousingRepository;
import com.example.youth.DB.Policy;
import com.example.youth.DB.Housing;
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
    private final HousingRepository housingRepository;

    @Value("${gemini.api.key:}")
    private String apiKey;  // 환경 변수에서만 로드 (기본값 없음)

    @Value("${gemini.api.model:gemini-2.5-flash}")
    private String modelName;

    @Value("${gemini.api.base-url:https://generativelanguage.googleapis.com/v1}")
    private String baseUrl;

    @Autowired
    public GeminiService(
            ProfanityFilterService profanityFilterService,
            PolicyRepository policyRepository,
            HousingRepository housingRepository) {
        this.profanityFilterService = profanityFilterService;
        this.policyRepository = policyRepository;
        this.housingRepository = housingRepository;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        
        // API 키 로드 확인 로그
        System.out.println("Gemini API 초기화:");
        System.out.println("  - Base URL: " + baseUrl);
        System.out.println("  - Model: " + modelName);
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("  ⚠️ WARNING: GEMINI_API_KEY 환경 변수가 설정되지 않았습니다!");
            System.err.println("  환경 변수 설정 방법:");
            System.err.println("    Windows: set GEMINI_API_KEY=your_api_key_here");
            System.err.println("    Linux/Mac: export GEMINI_API_KEY=your_api_key_here");
        } else {
            System.out.println("  - API Key: " + apiKey.substring(0, Math.min(10, apiKey.length())) + "... (로드됨)");
        }
    }

    /**
     * Gemini API를 사용하여 챗봇 응답 생성
     * @param message 사용자 메시지
     * @param userId 사용자 ID (선택)
     * @return 챗봇 응답
     */
    public Mono<ChatResponse> generateChatResponse(String message, String userId) {
        // 1. 비속어 필터링
        if (profanityFilterService.containsProfanity(message)) {
            return Mono.just(ChatResponse.builder()
                    .response("부적절한 언어가 포함되어 있습니다. 정중한 표현으로 다시 질문해주세요.")
                    .actionLinks(new ArrayList<>())
                    .build());
        }

        // 2. 시스템 프롬프트 생성
        String systemPrompt = buildSystemPrompt();

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
     */
    private String buildSystemPrompt() {
        // 사용 가능한 정책 목록 가져오기 (최근 활성 정책 10개)
        List<Policy> recentPolicies = policyRepository.findAll().stream()
                .limit(10)
                .toList();
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("당신은 '슬기로운 청년생활' 앱의 청년정책 전문 상담 챗봇입니다. ");
        prompt.append("사용자의 질문에 친절하고 정확하게 답변해주세요. ");
        prompt.append("청년정책, 주거 지원, 취업 지원, 창업 지원 등에 대한 정보를 제공할 수 있습니다. ");
        prompt.append("답변은 간결하고 이해하기 쉽게 작성해주세요.\n\n");
        
        if (!recentPolicies.isEmpty()) {
            prompt.append("사용 가능한 정책 정보:\n");
            for (Policy policy : recentPolicies) {
                prompt.append(String.format("- 정책 ID: %s, 제목: %s, 카테고리: %s\n", 
                    policy.getPolicyId(), policy.getTitle(), policy.getCategory()));
            }
            prompt.append("\n정책을 언급할 때는 반드시 [정책ID:정책ID값] 형식으로 ID를 포함해주세요. ");
            prompt.append("예: '청년 주거 지원 정책이 있습니다 [정책ID:policy123]'\n\n");
        }
        
        prompt.append("주택 정보를 언급할 때는 [주택ID:주택ID값] 형식으로 ID를 포함해주세요.");
        
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
                    
                    return ChatResponse.builder()
                            .response(text)
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
                links.add(ChatResponse.ActionLink.builder()
                        .type("policy")
                        .id(policyId)
                        .title(policy.getTitle())
                        .build());
            }
        }
        
        // 주택 ID 패턴: [주택ID:housing123] 또는 [주택ID:housing-123]
        Pattern housingPattern = Pattern.compile("\\[주택ID:([^\\]]+)\\]", Pattern.CASE_INSENSITIVE);
        Matcher housingMatcher = housingPattern.matcher(response);
        
        while (housingMatcher.find()) {
            String housingId = housingMatcher.group(1).trim();
            Housing housing = housingRepository.findById(housingId).orElse(null);
            
            if (housing != null) {
                links.add(ChatResponse.ActionLink.builder()
                        .type("housing")
                        .id(housingId)
                        .title(housing.getName())
                        .build());
            }
        }
        
        return links;
    }
}

