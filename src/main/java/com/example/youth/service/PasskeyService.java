package com.example.youth.service;

import com.example.youth.DB.User;
import com.example.youth.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Passkey 검증 및 로그인 처리 서비스
 */
@Service
public class PasskeyService {
    
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    
    public PasskeyService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Passkey credential에서 사용자 정보 추출
     * @param credentialJson Passkey credential JSON 문자열
     * @return 사용자 정보 (userId, email 등)
     */
    public Map<String, String> extractUserInfoFromCredential(String credentialJson) {
        try {
            JsonNode credential = objectMapper.readTree(credentialJson);
            
            // response에서 userHandle 추출
            JsonNode response = credential.get("response");
            if (response == null || !response.has("userHandle")) {
                return null;
            }
            
            String userHandleBase64 = response.get("userHandle").asText();
            if (userHandleBase64 == null || userHandleBase64.isEmpty()) {
                return null;
            }
            
            // userHandle은 Base64로 인코딩된 사용자 ID (이메일)
            byte[] userHandleBytes = Base64.getUrlDecoder().decode(userHandleBase64);
            String userHandle = new String(userHandleBytes);
            
            Map<String, String> userInfo = new HashMap<>();
            userInfo.put("userHandle", userHandle);
            
            // userHandle이 이메일인 경우
            if (userHandle.contains("@")) {
                userInfo.put("email", userHandle);
            }
            
            return userInfo;
            
        } catch (Exception e) {
            System.err.println("Passkey credential 파싱 실패: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Passkey로 사용자 조회
     * @param credentialJson Passkey credential JSON 문자열
     * @return 사용자 정보 (없으면 null)
     */
    public User findUserByPasskey(String credentialJson) {
        try {
            Map<String, String> userInfo = extractUserInfoFromCredential(credentialJson);
            if (userInfo == null) {
                return null;
            }
            
            String email = userInfo.get("email");
            if (email == null || email.isEmpty()) {
                return null;
            }
            
            // 이메일로 사용자 조회
            return userRepository.findByEmail(email).orElse(null);
            
        } catch (Exception e) {
            System.err.println("Passkey로 사용자 조회 실패: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Passkey credential 검증 (간단한 버전)
     * 실제 프로덕션에서는 WebAuthn 라이브러리를 사용하여 완전한 검증 필요
     * 
     * @param credentialJson Passkey credential JSON 문자열
     * @return 검증 성공 여부
     */
    public boolean verifyCredential(String credentialJson) {
        try {
            JsonNode credential = objectMapper.readTree(credentialJson);
            
            // 필수 필드 확인
            if (!credential.has("id") || !credential.has("rawId") || !credential.has("response")) {
                return false;
            }
            
            JsonNode response = credential.get("response");
            if (!response.has("authenticatorData") || 
                !response.has("clientDataJSON") || 
                !response.has("signature")) {
                return false;
            }
            
            // TODO: 실제 검증 로직 구현
            // 1. challenge 검증 (서버에서 생성한 challenge와 일치하는지)
            // 2. rpId 검증
            // 3. 서명 검증 (공개키로 서명 검증)
            // 4. userHandle 검증
            
            // 현재는 기본적인 형식 검증만 수행
            return true;
            
        } catch (Exception e) {
            System.err.println("Passkey credential 검증 실패: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}

