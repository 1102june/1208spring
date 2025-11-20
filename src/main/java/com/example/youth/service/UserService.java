package com.example.youth.service;

import com.example.youth.db.User;
import com.example.youth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service // 이 파일은 @Service가 맞습니다.
public class UserService {

    @Autowired
    private UserRepository userRepository; // Spring이 자동으로 UserRepository를 주입합니다.

    /**
     * 회원가입 로직
     * (UserController에서 호출)
     */
    public String registerUser(User user) {
        // 1. 이메일 중복 확인
        if (userRepository.existsByEmail(user.getEmail())) {
            return "Email already exists";
        }

        // 2. 중복이 없으면 DB에 저장
        userRepository.save(user);
        return "회원가입 성공";
    }

    /**
     * UID로 사용자 정보 조회 로직
     * (기존 controller/UserService에 있던 기능)
     */
    public User getUserByUid(String uid) {
        // 1. UID로 사용자를 찾아보고, 없으면 null 반환
        return userRepository.findById(uid).orElse(null);
    }
}