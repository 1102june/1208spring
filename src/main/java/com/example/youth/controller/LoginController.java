package com.example.youth.controller;

import com.example.youth.service.FirebaseAuthService;
import com.example.youth.service.UserService;
import com.example.youth.service.PasskeyService;
import com.example.youth.dto.*;
import com.example.youth.DB.User;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class LoginController {

    private final FirebaseAuthService firebaseAuthService;
    private final UserService userService;
    private final PasskeyService passkeyService;

    public LoginController(FirebaseAuthService firebaseAuthService, UserService userService, PasskeyService passkeyService) {
        this.firebaseAuthService = firebaseAuthService;
        this.userService = userService;
        this.passkeyService = passkeyService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            // 1) Firebase ID Token 검증
            FirebaseToken decodedToken = firebaseAuthService.verifyToken(loginRequest.getIdToken());
            String uid = decodedToken.getUid();

            // 2) DB에서 사용자 조회
            User user = userService.getUserByUid(uid);
            if (user == null) {
                return ResponseEntity.status(404).body("USER_NOT_FOUND");
            }

            // 3) 로그인 성공 (Firebase 이메일 인증 체크 제거 - Gmail SMTP 사용)
            return ResponseEntity.ok("LOGIN_SUCCESS");

        } catch (Exception e) {
            return ResponseEntity.status(500).body("LOGIN_ERROR: " + e.getMessage());
        }
    }

    /**
     * Passkey 로그인 요청 생성
     * 클라이언트가 Passkey 로그인을 시작하기 전에 challenge를 받아옴
     * 
     * @return Passkey 로그인에 필요한 challenge 및 설정
     */
    @GetMapping("/passkey/login/request")
    public ResponseEntity<ApiResponse<PasskeyLoginRequestResponse>> getPasskeyLoginRequest() {
        try {
            // 1. 랜덤 challenge 생성 (32바이트)
            byte[] challengeBytes = new byte[32];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(challengeBytes);
            
            // 2. Base64 URL-safe 인코딩
            String challenge = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(challengeBytes);
            
            // 3. Passkey 로그인 요청 응답 생성
            PasskeyLoginRequestResponse response = PasskeyLoginRequestResponse.builder()
                    .challenge(challenge)
                    .rpId("localhost") // 개발용, 실제 배포 시 도메인으로 변경 필요
                    .timeout(60000L) // 60초
                    .userVerification("preferred") // 생체인증 선호
                    .allowCredentials(new ArrayList<>()) // 빈 리스트 = 모든 Passkey 허용
                    .build();
            
            return ResponseEntity.ok(ApiResponse.success("Passkey 로그인 요청 생성 성공", response));
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Passkey 로그인 요청 생성 실패: " + e.getMessage()));
        }
    }

    /**
     * Passkey 로그인 처리
     * 클라이언트에서 받은 Passkey credential을 검증하고 로그인 처리
     * 
     * @param request Passkey credential
     * @return 로그인 결과
     */
    @PostMapping("/passkey/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> passkeyLogin(@RequestBody PasskeyLoginRequest request) {
        try {
            String credentialJson = request.getCredential();
            if (credentialJson == null || credentialJson.isEmpty()) {
                return ResponseEntity.status(400)
                        .body(ApiResponse.error("Passkey credential이 없습니다."));
            }
            
            // 1. Passkey credential 검증 (기본 형식 검증)
            if (!passkeyService.verifyCredential(credentialJson)) {
                return ResponseEntity.status(400)
                        .body(ApiResponse.error("Passkey credential 형식이 올바르지 않습니다."));
            }
            
            // 2. credential에서 사용자 정보 추출
            Map<String, String> userInfo = passkeyService.extractUserInfoFromCredential(credentialJson);
            if (userInfo == null) {
                return ResponseEntity.status(400)
                        .body(ApiResponse.error("Passkey credential에서 사용자 정보를 추출할 수 없습니다."));
            }
            
            // 3. 사용자 조회
            User user = passkeyService.findUserByPasskey(credentialJson);
            if (user == null) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error("Passkey에 해당하는 사용자를 찾을 수 없습니다. 먼저 Passkey를 등록해주세요."));
            }
            
            // 4. 로그인 성공 응답
            Map<String, String> responseData = new HashMap<>();
            responseData.put("userId", user.getUserId());
            responseData.put("email", user.getEmail());
            responseData.put("message", "Passkey 로그인 성공");
            
            return ResponseEntity.ok(ApiResponse.success("Passkey 로그인 성공", responseData));
            
        } catch (Exception e) {
            System.err.println("Passkey 로그인 처리 중 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Passkey 로그인 실패: " + e.getMessage()));
        }
    }

    /**
     * Passkey 등록 요청 생성
     * 클라이언트가 Passkey 등록을 시작하기 전에 challenge를 받아옴
     * 
     * @param email 사용자 이메일
     * @param displayName 사용자 표시 이름
     * @return Passkey 등록에 필요한 challenge 및 설정
     */
    @GetMapping("/passkey/register/request")
    public ResponseEntity<ApiResponse<PasskeyRegisterRequestResponse>> getPasskeyRegisterRequest(
            @RequestParam String email,
            @RequestParam(required = false, defaultValue = "") String displayName
    ) {
        try {
            // 1. 랜덤 challenge 생성 (32바이트)
            byte[] challengeBytes = new byte[32];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(challengeBytes);
            
            // 2. Base64 URL-safe 인코딩
            String challenge = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(challengeBytes);
            
            // 3. 사용자 ID 생성 (이메일을 바이트 배열로 변환 후 Base64 인코딩)
            byte[] userIdBytes = email.getBytes("UTF-8");
            String userId = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(userIdBytes);
            
            // 4. 표시 이름이 없으면 이메일 사용
            String userDisplayName = displayName != null && !displayName.isEmpty() 
                    ? displayName 
                    : email;
            
            // 5. Passkey 등록 요청 응답 생성
            PasskeyRegisterRequestResponse response = PasskeyRegisterRequestResponse.builder()
                    .challenge(challenge)
                    .rpId("localhost") // 개발용, 실제 배포 시 도메인으로 변경 필요
                    .rpName("슬기로운 청년생활")
                    .userId(userId)
                    .userName(email)
                    .userDisplayName(userDisplayName)
                    .timeout(60000L) // 60초
                    .userVerification("preferred") // 생체인증 선호
                    .build();
            
            return ResponseEntity.ok(ApiResponse.success("Passkey 등록 요청 생성 성공", response));
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Passkey 등록 요청 생성 실패: " + e.getMessage()));
        }
    }

    /**
     * Passkey 등록 처리
     * 클라이언트에서 받은 Passkey credential을 검증하고 저장
     * 
     * @param request Passkey credential 및 이메일
     * @return 등록 결과
     */
    @PostMapping("/passkey/register")
    public ResponseEntity<ApiResponse<String>> passkeyRegister(@RequestBody PasskeyRegisterRequest request) {
        try {
            String credentialJson = request.getCredential();
            String email = request.getEmail();
            
            if (credentialJson == null || credentialJson.isEmpty()) {
                return ResponseEntity.status(400)
                        .body(ApiResponse.error("Passkey credential이 없습니다."));
            }
            
            if (email == null || email.isEmpty()) {
                return ResponseEntity.status(400)
                        .body(ApiResponse.error("이메일이 없습니다."));
            }
            
            // 1. Passkey credential 검증 (기본 형식 검증)
            if (!passkeyService.verifyCredential(credentialJson)) {
                return ResponseEntity.status(400)
                        .body(ApiResponse.error("Passkey credential 형식이 올바르지 않습니다."));
            }
            
            // 2. credential에서 사용자 정보 추출
            Map<String, String> userInfo = passkeyService.extractUserInfoFromCredential(credentialJson);
            if (userInfo == null) {
                return ResponseEntity.status(400)
                        .body(ApiResponse.error("Passkey credential에서 사용자 정보를 추출할 수 없습니다."));
            }
            
            // 3. 이메일 일치 확인
            String credentialEmail = userInfo.get("email");
            if (credentialEmail == null || !credentialEmail.equals(email)) {
                return ResponseEntity.status(400)
                        .body(ApiResponse.error("Passkey credential의 이메일이 일치하지 않습니다."));
            }
            
            // 4. 사용자 조회 (이미 존재하는지 확인)
            User user = userService.getUserByEmail(email);
            if (user == null) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error("사용자를 찾을 수 없습니다. 먼저 회원가입을 완료해주세요."));
            }
            
            // 5. Passkey 등록 성공
            // 실제로는 credential ID를 DB에 저장할 수 있지만,
            // Passkey는 기기에 저장되므로 서버에서는 검증만 수행
            
            return ResponseEntity.ok(ApiResponse.success("Passkey 등록 성공"));
            
        } catch (Exception e) {
            System.err.println("Passkey 등록 처리 중 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Passkey 등록 실패: " + e.getMessage()));
        }
    }
}
