package com.example.youth.repository;

import com.example.youth.DB.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByEmail(String email); // 이메일 중복 확인
    
    // 이메일로 사용자 조회 (중복 시 첫 번째만 반환)
    @Query(value = "SELECT * FROM user WHERE email = :email LIMIT 1", nativeQuery = true)
    Optional<User> findByEmail(@Param("email") String email);
    
    // 사용자 ID로 조회 (userId가 Primary Key이므로 findById를 사용)
    // 하위 호환성을 위해 유지하되, 실제로는 findById와 동일
    // JpaRepository<User, String>이므로 findById(String) 자동 제공
    default Optional<User> findByUserId(String userId) {
        return findById(userId);
    }
}
