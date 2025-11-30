package com.example.youth.controller;

import com.example.youth.dto.ApiResponse;
import com.example.youth.dto.BookmarkRequest;
import com.example.youth.dto.BookmarkResponse;
import com.example.youth.service.BookmarkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookmarks")
public class BookmarkController {

    @Autowired
    private BookmarkService bookmarkService;

    @PostMapping
    public ResponseEntity<ApiResponse<BookmarkResponse>> addBookmark(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody BookmarkRequest request) {
        try {
            BookmarkResponse response = bookmarkService.addBookmark(userId, request);
            return ResponseEntity.ok(ApiResponse.success("북마크가 추가되었습니다.", response));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{bookmarkId}")
    public ResponseEntity<ApiResponse<Void>> deleteBookmark(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable Long bookmarkId) {
        try {
            bookmarkService.deleteBookmark(bookmarkId);
            return ResponseEntity.ok(ApiResponse.success("북마크가 삭제되었습니다.", null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BookmarkResponse>>> getBookmarks(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(required = false) String contentType) {
        try {
            List<BookmarkResponse> responses = bookmarkService.getBookmarks(userId, contentType);
            return ResponseEntity.ok(ApiResponse.success("북마크 목록 조회 성공", responses));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(e.getMessage()));
        }
    }

    // 관리자용: 모든 북마크 삭제
    @DeleteMapping("/admin/all")
    public ResponseEntity<ApiResponse<String>> deleteAllBookmarks() {
        try {
            bookmarkService.deleteAllBookmarks();
            return ResponseEntity.ok(ApiResponse.success("모든 북마크가 삭제되었습니다.", null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(e.getMessage()));
        }
    }
}

