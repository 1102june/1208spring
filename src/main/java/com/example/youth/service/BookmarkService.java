package com.example.youth.service;

import com.example.youth.DB.ActiveStatus;
import com.example.youth.DB.Bookmark;
import com.example.youth.DB.User;
import com.example.youth.common.ContentType;
import com.example.youth.dto.BookmarkRequest;
import com.example.youth.dto.BookmarkResponse;
import com.example.youth.repository.BookmarkRepository;
import com.example.youth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookmarkService {

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public BookmarkResponse addBookmark(String userId, BookmarkRequest request) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        ContentType contentType = ContentType.valueOf(request.getContentType());

        // 중복 확인 (이미 존재하는 경우 활성화만 변경하거나 무시)
        // 여기서는 단순 추가 로직으로 구현 (중복 시 에러 또는 기존 것 반환 처리 필요 시 수정)
        
        Bookmark bookmark = Bookmark.builder()
                .user(user)
                .contentType(contentType)
                .contentId(request.getContentId())
                .isActive(ActiveStatus.Y)
                .build();

        Bookmark saved = bookmarkRepository.save(bookmark);

        return BookmarkResponse.builder()
                .bookmarkId(saved.getBookmarkId())
                .userId(user.getUserId())
                .contentType(saved.getContentType().name())
                .contentId(saved.getContentId())
                .isActive(saved.getIsActive().name())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Transactional
    public void deleteBookmark(Long bookmarkId) {
        Bookmark bookmark = bookmarkRepository.findById(bookmarkId)
                .orElseThrow(() -> new IllegalArgumentException("북마크를 찾을 수 없습니다: " + bookmarkId));
        
        // 실제 삭제 (데이터베이스에서 완전히 제거)
        bookmarkRepository.delete(bookmark);
    }

    @Transactional
    public void deleteAllBookmarks() {
        bookmarkRepository.deleteAll();
    }

    @Transactional(readOnly = true)
    public List<BookmarkResponse> getBookmarks(String userId, String contentType) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        List<Bookmark> bookmarks = bookmarkRepository.findByUser(user);

        return bookmarks.stream()
                .filter(b -> b.getIsActive() == ActiveStatus.Y)
                .filter(b -> contentType == null || b.getContentType().name().equals(contentType))
                .map(b -> BookmarkResponse.builder()
                        .bookmarkId(b.getBookmarkId())
                        .userId(b.getUser().getUserId())
                        .contentType(b.getContentType().name())
                        .contentId(b.getContentId())
                        .isActive(b.getIsActive().name())
                        .createdAt(b.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}

