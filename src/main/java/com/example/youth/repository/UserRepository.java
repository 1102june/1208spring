package com.example.youth.repository;

import com.example.youth.DB.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByEmail(String email); // 이메일 중복 확인
    Optional<User> findByEmail(String email); // 이메일로 사용자 조회
}
