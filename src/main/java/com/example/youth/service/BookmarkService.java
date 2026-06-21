package com.example.youth.service;

import com.example.youth.DB.ActiveStatus;
import com.example.youth.DB.Bookmark;
import com.example.youth.DB.User;
import com.example.youth.common.ContentType;
import com.example.youth.dto.BookmarkRequest;
import com.example.youth.dto.BookmarkResponse;
import com.example.youth.dto.HousingResponse;
import com.example.youth.dto.PolicyResponse;
import com.example.youth.repository.BookmarkRepository;
import com.example.youth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class BookmarkService {

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    @Lazy
    private PolicyService policyService;

    @Autowired
    @Lazy
    private HousingService housingService;

    @Transactional
    public BookmarkResponse addBookmark(String userId, BookmarkRequest request) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        ContentType contentType = parseContentType(request.getContentType());
        String contentId = request.getContentId();

        var existing = bookmarkRepository.findByUser_UserIdAndContentTypeAndContentId(
                userId, contentType, contentId);
        if (existing.isPresent()) {
            Bookmark bookmark = existing.get();
            bookmark.setIsActive(ActiveStatus.Y);
            Bookmark saved = bookmarkRepository.save(bookmark);
            return toResponse(saved, userId);
        }

        Bookmark bookmark = Bookmark.builder()
                .user(user)
                .contentType(contentType)
                .contentId(contentId)
                .isActive(ActiveStatus.Y)
                .build();

        Bookmark saved = bookmarkRepository.save(bookmark);
        return toResponse(saved, userId);
    }

    @Transactional
    public void deleteBookmark(Long bookmarkId) {
        Bookmark bookmark = bookmarkRepository.findById(bookmarkId)
                .orElseThrow(() -> new IllegalArgumentException("북마크를 찾을 수 없습니다: " + bookmarkId));
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

        ContentType filterType = null;
        if (contentType != null && !contentType.isBlank()) {
            filterType = parseContentType(contentType);
        }

        final ContentType typeFilter = filterType;
        List<Bookmark> bookmarks = bookmarkRepository.findByUser(user);

        return bookmarks.stream()
                .filter(b -> b.getIsActive() == ActiveStatus.Y)
                .filter(b -> typeFilter == null || b.getContentType() == typeFilter)
                .map(b -> toResponse(b, userId))
                .collect(Collectors.toList());
    }

    private BookmarkResponse toResponse(Bookmark bookmark, String userId) {
        BookmarkResponse.BookmarkResponseBuilder builder = BookmarkResponse.builder()
                .bookmarkId(bookmark.getBookmarkId())
                .userId(bookmark.getUser().getUserId())
                .contentType(bookmark.getContentType().name())
                .contentId(bookmark.getContentId())
                .isActive(bookmark.getIsActive().name())
                .createdAt(bookmark.getCreatedAt());

        if (bookmark.getContentType() == ContentType.policy) {
            PolicyResponse policy = policyService.toPolicyResponse(bookmark.getContentId(), userId);
            if (policy != null) {
                policy.setIsBookmarked(true);
                builder.policy(policy);
            }
        } else if (bookmark.getContentType() == ContentType.housing) {
            try {
                HousingResponse housing = housingService.getHousingById(bookmark.getContentId(), userId);
                if (housing != null) {
                    housing.setIsBookmarked(true);
                    builder.housing(housing);
                }
            } catch (Exception e) {
                System.err.println("북마크 임대주택 조회 실패 contentId=" + bookmark.getContentId()
                        + ": " + e.getMessage());
            }
        }

        return builder.build();
    }

    private ContentType parseContentType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("contentType이 필요합니다 (policy 또는 housing)");
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return ContentType.valueOf(normalized);
    }
}
