package com.example.youth.controller;

import com.example.youth.dto.OtpRequest;
import com.example.youth.service.OtpService;
import com.example.youth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/otp")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;
    private final UserService userService;

    /** 1) 인증번호 발송 */
    @PostMapping("/send")
    public ResponseEntity<?> sendOtp(@RequestBody OtpRequest request) {
        otpService.generateAndSendOtp(request.getEmail());
        return ResponseEntity.ok("OTP_SENT");
    }

    /** 2) 인증번호 검증 */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyOtp(@RequestBody OtpRequest request) {

        boolean valid = otpService.verifyOtp(request.getEmail(), request.getOtp());

        if (!valid) {
            return ResponseEntity.status(400).body("INVALID_OTP");
        }

        // 이메일 인증 완료 처리
        userService.updateEmailVerified(request.getEmail());

        return ResponseEntity.ok("OTP_VERIFIED");
    }

    /** 3) 인증 여부 확인 (선택 기능) */
    @GetMapping("/status")
    public ResponseEntity<?> isVerified(@RequestParam String email) {
        boolean verified = userService.isEmailVerified(email);
        return ResponseEntity.ok(verified);
    }
}
