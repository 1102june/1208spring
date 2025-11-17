package com.example.youth.service;

import com.example.youth.DB.Policy;
import com.example.youth.dto.PolicyResponse;
import com.example.youth.repository.BookmarkRepository;
import com.example.youth.repository.PolicyRepository;
import com.example.youth.common.ContentType;
import com.example.youth.DB.ActiveStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PolicyService {

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    // 정책을 PolicyResponse로 변환 (북마크 여부 포함)
    private PolicyResponse convertToResponse(Policy policy, String userId) {
        boolean isBookmarked = bookmarkRepository
                .findByUser_UserIdAndContentTypeAndContentId(userId, ContentType.policy, policy.getPolicyId())
                .map(bookmark -> bookmark.getIsActive() == ActiveStatus.Y)
                .orElse(false);

        return PolicyResponse.builder()
                .policyId(policy.getPolicyId())
                .title(policy.getTitle())
                .summary(policy.getSummary())
                .category(policy.getCategory())
                .region(policy.getRegion())
                .ageStart(policy.getAgeStart())
                .ageEnd(policy.getAgeEnd())
                .eligibility(policy.getEligibility())
                .applicationStart(policy.getApplicationStart())
                .applicationEnd(policy.getApplicationEnd())
                .link1(policy.getLink1())
                .link2(policy.getLink2())
                .isBookmarked(isBookmarked)
                .build();
    }

    // ID로 정책 조회
    public PolicyResponse getPolicyById(String policyId, String userId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("정책을 찾을 수 없습니다: " + policyId));
        return convertToResponse(policy, userId);
    }

    // 활성 정책 목록 조회
    public List<PolicyResponse> getActivePolicies(String userId) {
        Date currentDate = new Date();
        List<Policy> policies = policyRepository.findActivePolicies(currentDate);
        return policies.stream()
                .map(policy -> convertToResponse(policy, userId))
                .collect(Collectors.toList());
    }

    // 나이에 맞는 정책 조회
    public List<PolicyResponse> getPoliciesByAge(Integer age, String userId) {
        List<Policy> policies = policyRepository.findPoliciesByAge(age);
        return policies.stream()
                .map(policy -> convertToResponse(policy, userId))
                .collect(Collectors.toList());
    }
}

