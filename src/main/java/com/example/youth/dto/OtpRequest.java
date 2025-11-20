package com.example.youth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtpRequest {
    private String email;
    private String otp; // verify에서만 사용됨
}
