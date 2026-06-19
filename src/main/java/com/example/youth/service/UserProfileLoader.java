package com.example.youth.service;

import com.example.youth.DB.InterestCategory;
import com.example.youth.DB.UserProfile;
import com.example.youth.dto.UserProfileResponse;
import com.example.youth.repository.InterestCategoryRepository;
import com.example.youth.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/** 프로필 조회 전용 (순환 참조 방지). */
@Component
public class UserProfileLoader {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private InterestCategoryRepository interestCategoryRepository;

    public UserProfileResponse loadProfile(String userId) {
        try {
            List<UserProfile> profiles = userProfileRepository.findAllByUser_UserId(userId);
            if (profiles.isEmpty()) {
                return null;
            }
            UserProfile profile = profiles.get(0);

            Integer age = null;
            if (profile.getBirthYear() != null) {
                age = (int) ChronoUnit.YEARS.between(profile.getBirthYear(), LocalDate.now());
            }

            List<String> interests = interestCategoryRepository.findByUser_UserId(userId).stream()
                    .map(InterestCategory::getCategory)
                    .collect(Collectors.toList());

            return UserProfileResponse.builder()
                    .userId(userId)
                    .nickname(profile.getNickname())
                    .age(age)
                    .region(profile.getRegion())
                    .education(profile.getEducation())
                    .jobStatus(profile.getJobStatus())
                    .interests(interests)
                    .build();
        } catch (Exception e) {
            System.err.println("UserProfileLoader: 프로필 조회 실패 userId=" + userId + " - " + e.getMessage());
            return null;
        }
    }
}
