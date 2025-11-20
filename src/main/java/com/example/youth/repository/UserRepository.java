package com.example.youth.repository;

import com.example.youth.db.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByEmail(String email); // 이메일 중복 확인
}
