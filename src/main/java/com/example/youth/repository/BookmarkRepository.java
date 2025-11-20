package com.example.youth.repository;

import com.example.youth.DB.Bookmark;
import com.example.youth.DB.ActiveStatus;
import com.example.youth.common.ContentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    
    // 사용자별 활성 북마크 조회
    List<Bookmark> findByUser_UserIdAndIsActiveOrderByCreatedAtDesc(String userId, ActiveStatus isActive);
    
    // 사용자별 특정 타입의 북마크 조회
    List<Bookmark> findByUser_UserIdAndContentTypeAndIsActive(String userId, ContentType contentType, ActiveStatus isActive);
    
    // 북마크 존재 여부 확인
    Optional<Bookmark> findByUser_UserIdAndContentTypeAndContentId(String userId, ContentType contentType, String contentId);
}

