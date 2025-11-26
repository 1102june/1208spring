package com.example.youth.service;

import com.example.youth.dto.ChatResponse;
import com.example.youth.repository.PolicyRepository;
import com.example.youth.repository.HousingRepository;
import com.example.youth.repository.ChatHistoryRepository;
import com.example.youth.DB.Policy;
import com.example.youth.DB.Housing;
import com.example.youth.DB.ChatHistory;
import com.example.youth.DB.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
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
    private final ChatHistoryRepository chatHistoryRepository;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-2.5-flash}")
    private String modelName;

    @Value("${gemini.api.base-url:https://generativelanguage.googleapis.com/v1}")
    private String baseUrl;

    @Autowired
    public GeminiService(
            ProfanityFilterService profanityFilterService,
            PolicyRepository policyRepository,
            HousingRepository housingRepository,
            ChatHistoryRepository chatHistoryRepository) {
        this.profanityFilterService = profanityFilterService;
        this.policyRepository = policyRepository;
        this.housingRepository = housingRepository;
        this.chatHistoryRepository = chatHistoryRepository;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        // API 키 검증
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException(
                "GEMINI_API_KEY 환경 변수가 설정되지 않았습니다. " +
                "Gemini API 키를 생성하고 환경 변수로 설정해주세요. " +
                "https://aistudio.google.com/app/apikey 에서 API 키를 생성할 수 있습니다."
            );
        }
        
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
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
        String url = String.format("/models/%s:generateContent?key=%s", modelName, apiKey);
        
        return webClient.post()
                .uri(url)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), 
                    response -> {
                        return response.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                System.err.println("Gemini API Error: " + errorBody);
                                return Mono.error(new RuntimeException("Gemini API Error: " + errorBody));
                            });
                    })
                .bodyToMono(String.class)
                .map(response -> {
                    System.out.println("Gemini API Response: " + response);
                    ChatResponse chatResponse = parseGeminiResponse(response, message);
                    
                    // 대화 기록 저장 (사용자 ID가 있는 경우에만)
                    if (userId != null && !userId.trim().isEmpty()) {
                        saveChatHistory(userId, message, chatResponse);
                    }
                    
                    return chatResponse;
                })
                .onErrorResume(e -> {
                    // 에러 발생 시 기본 응답 반환
                    e.printStackTrace();
                    ChatResponse errorResponse = ChatResponse.builder()
                            .response("죄송합니다. 일시적인 오류가 발생했습니다: " + e.getMessage())
                            .actionLinks(new ArrayList<>())
                            .build();
                    
                    // 에러 응답도 기록 저장 (사용자 ID가 있는 경우에만)
                    if (userId != null && !userId.trim().isEmpty()) {
                        saveChatHistory(userId, message, errorResponse);
                    }
                    
                    return Mono.just(errorResponse);
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

    /**
     * 챗봇 대화 기록 저장
     */
    private void saveChatHistory(String userId, String userMessage, ChatResponse chatResponse) {
        try {
            // 액션 링크를 JSON 형태로 변환
            String actionLinksJson = null;
            if (chatResponse.getActionLinks() != null && !chatResponse.getActionLinks().isEmpty()) {
                actionLinksJson = objectMapper.writeValueAsString(chatResponse.getActionLinks());
            }

            // User 엔티티 조회 (간단한 참조만 필요하므로 프록시로 충분)
            User user = new User();
            user.setUserId(userId);

            ChatHistory chatHistory = ChatHistory.builder()
                    .user(user)
                    .userMessage(userMessage)
                    .botResponse(chatResponse.getResponse())
                    .actionLinks(actionLinksJson)
                    .createdAt(java.time.LocalDateTime.now())
                    .build();

            chatHistoryRepository.save(chatHistory);
        } catch (Exception e) {
            // 대화 기록 저장 실패는 로그만 남기고 계속 진행
            System.err.println("챗봇 대화 기록 저장 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

