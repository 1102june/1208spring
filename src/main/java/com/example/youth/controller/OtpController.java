package com.example.youth.controller;

import com.example.youth.dto.ApiResponse;
import com.example.youth.dto.OtpRequest;
import com.example.youth.service.OtpService;
import com.example.youth.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * OTP(이메일 인증번호) API 컨트롤러
 * 
 * 제공 기능:
 * 1. GET /auth/otp/email/check - 이메일 중복 확인
 * 2. POST /auth/otp/send - 인증번호 발송 (중복 확인 포함)
 * 3. POST /auth/otp/verify - 인증번호 검증
 * 4. GET /auth/otp/status - 이메일 인증 여부 확인
 */
@Slf4j
@RestController
@RequestMapping("/auth/otp")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;
    private final UserService userService;

    /**
     * 이메일 중복 확인 API
     * 
     * @param email 확인할 이메일 주소
     * @return 중복 여부 (true: 이미 사용 중, false: 사용 가능)
     * 
     * 사용 시나리오:
     * - 회원가입 전 이메일 중복 확인
     * - 인증번호 발송 전 중복 확인
     */
    @GetMapping("/email/check")
    public ResponseEntity<ApiResponse<Boolean>> checkEmailDuplicate(@RequestParam String email) {
        try {
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("이메일 주소를 입력해주세요."));
            }

            String emailLower = email.trim().toLowerCase();
            
            // 이메일 중복 확인
            boolean exists = userService.isEmailExists(emailLower);

            if (exists) {
                log.info("이메일 중복 확인: {} (이미 사용 중)", emailLower);
                return ResponseEntity.ok(ApiResponse.success(true));
            } else {
                log.info("이메일 중복 확인: {} (사용 가능)", emailLower);
                return ResponseEntity.ok(ApiResponse.success(false));
            }

        } catch (Exception e) {
            log.error("이메일 중복 확인 중 오류 발생: {}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("이메일 중복 확인 중 오류가 발생했습니다."));
        }
    }

    /**
     * 인증번호 발송 API
     * 
     * @param request 이메일 주소 포함
     * @return 성공/실패 응답
     * 
     * 동작 방식:
     * 1. 이메일 중복 확인 (이미 등록된 이메일이면 오류 반환)
     * 2. 이메일로 6자리 인증번호 발송
     * 3. 인증번호는 5분간 유효 (설정 변경 가능)
     * 4. 같은 이메일로 재발송 시 기존 인증번호는 무효화됨
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<String>> sendOtp(@RequestBody OtpRequest request) {
        String email = null;
        try {
            // 이메일 검증
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("이메일 주소를 입력해주세요."));
            }

            email = request.getEmail().trim().toLowerCase();

            // 이메일 중복 확인 (이미 등록된 이메일이면 오류)
            if (userService.isEmailExists(email)) {
                log.warn("이미 등록된 이메일로 인증번호 발송 시도: {}", email);
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("이미 등록된 이메일 주소입니다."));
            }

            // 인증번호 생성 및 발송
            try {
                otpService.generateAndSendOtp(email);
                log.info("✅ 인증번호 발송 성공: {}", email);
                return ResponseEntity.ok(ApiResponse.success("인증번호가 이메일로 전송되었습니다. 이메일을 확인해주세요."));
            } catch (RuntimeException e) {
                // 이메일 발송 실패 시에도 인증번호는 생성되었으므로, 콘솔에 출력된 인증번호를 사용할 수 있음
                String errorMessage = e.getMessage();
                if (errorMessage.contains("EmailService가 설정되지 않았습니다") || 
                    errorMessage.contains("이메일 발송에 실패했습니다")) {
                    log.warn("⚠️ 이메일 발송 실패 또는 EmailService 미설정: {}. 콘솔 로그를 확인하세요.", email);
                    // 개발 환경에서는 성공으로 처리 (콘솔에 인증번호 출력됨)
                    return ResponseEntity.ok(ApiResponse.success("인증번호가 생성되었습니다. 서버 콘솔을 확인하세요 (개발 모드)."));
                }
                // 다른 오류는 그대로 throw
                throw e;
            }

        } catch (IllegalArgumentException e) {
            log.warn("인증번호 발송 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error("인증번호 발송 중 오류 발생: {}", email != null ? email : "unknown", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("인증번호 발송에 실패했습니다. 잠시 후 다시 시도해주세요."));
        }
    }

    /**
     * 인증번호 검증 API
     * 
     * @param request 이메일과 인증번호 포함
     * @return 검증 성공/실패 응답
     * 
     * 동작 방식:
     * 1. 입력된 인증번호와 저장된 인증번호 비교
     * 2. 만료 시간 확인
     * 3. 검증 성공 시:
     *    - 인증번호 삭제 (재사용 방지)
     *    - 사용자의 이메일 인증 상태 업데이트 (DB에 사용자가 존재하는 경우)
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<String>> verifyOtp(@RequestBody OtpRequest request) {
        try {
            // 입력값 검증
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("이메일 주소를 입력해주세요."));
            }

            if (request.getOtp() == null || request.getOtp().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("인증번호를 입력해주세요."));
            }

            String email = request.getEmail().trim().toLowerCase();
            String otp = request.getOtp().trim();

            // 인증번호 검증
            boolean isValid = otpService.verifyOtp(email, otp);

            if (!isValid) {
                log.warn("인증번호 검증 실패: {} (입력된 번호: {})", email, otp);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("인증번호가 일치하지 않거나 만료되었습니다."));
            }

            // 이메일 인증 완료 처리 (DB에 사용자가 존재하는 경우)
            try {
                userService.updateEmailVerified(email);
            } catch (RuntimeException e) {
                // 사용자가 아직 회원가입하지 않은 경우는 정상 (회원가입 시 인증 상태 업데이트)
                log.debug("사용자 정보 없음 (회원가입 전): {}", email);
            }

            log.info("인증번호 검증 성공: {}", email);

            return ResponseEntity.ok(ApiResponse.success("이메일 인증이 완료되었습니다."));

        } catch (Exception e) {
            log.error("인증번호 검증 중 오류 발생: {}", request.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("인증번호 검증 중 오류가 발생했습니다."));
        }
    }

    /**
     * 이메일 인증 여부 확인 API
     * 
     * @param email 확인할 이메일 주소
     * @return 인증 여부
     * 
     * 사용 시나리오:
     * - 회원가입 전 이미 인증된 이메일인지 확인
     * - 로그인 시 이메일 인증 완료 여부 확인
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Boolean>> isVerified(@RequestParam String email) {
        try {
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("이메일 주소를 입력해주세요."));
            }

            String emailLower = email.trim().toLowerCase();
            boolean verified = userService.isEmailVerified(emailLower);

            return ResponseEntity.ok(ApiResponse.success(verified));

        } catch (Exception e) {
            log.error("인증 여부 확인 중 오류 발생: {}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("인증 여부 확인 중 오류가 발생했습니다."));
        }
    }

    /**
     * @deprecated Google 재인증 + DELETE /auth/account (idToken) 사용
     */
    @Deprecated
    @PostMapping("/send/delete-account")
    public ResponseEntity<ApiResponse<String>> sendOtpForDeleteAccount(@RequestBody OtpRequest request) {
        return ResponseEntity.status(HttpStatus.GONE)
                .body(ApiResponse.error(
                        "회원탈퇴는 Google 재인증 후 DELETE /auth/account (body: idToken)을 사용해주세요."));
    }
}
