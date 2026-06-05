package com.example.youth.controller;

import com.example.youth.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("service", "UP");

        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(3);
            status.put("database", valid ? "UP" : "DOWN");
            if (!valid) {
                return ResponseEntity.status(503).body(fail("데이터베이스 연결 확인 실패", status));
            }
        } catch (Exception e) {
            status.put("database", "DOWN");
            status.put("databaseError", e.getMessage());
            return ResponseEntity.status(503)
                    .body(fail("데이터베이스 연결 실패: " + e.getMessage(), status));
        }

        return ResponseEntity.ok(ApiResponse.success("서버 정상", status));
    }

    private static ApiResponse<Map<String, Object>> fail(String message, Map<String, Object> status) {
        return ApiResponse.<Map<String, Object>>builder()
                .success(false)
                .message(message)
                .data(status)
                .build();
    }
}
