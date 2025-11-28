package com.example.youth.service;

import com.example.youth.DB.InterestCategory;
import com.example.youth.DB.User;
import com.example.youth.DB.UserProfile;
import com.example.youth.dto.ProfileRequest;
import com.example.youth.dto.UserProfileResponse;
import com.example.youth.repository.InterestCategoryRepository;
import com.example.youth.repository.UserProfileRepository;
import com.example.youth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final InterestCategoryRepository interestCategoryRepository;

    /**
     * 회원가입 로직
     * (SignupController에서 호출)
     */
    @Transactional
    public String registerUser(User user) {
        // UID로 이미 존재하는지 확인 (이메일 중복 체크는 제거 - Google 로그인 사용자는 같은 이메일로 여러 계정 가능)
        if (userRepository.existsById(user.getUserId())) {
            return "User already exists";
        }

        userRepository.save(user);
        userRepository.flush(); // 즉시 DB에 반영
        return "회원가입 성공";
    }

    /**
     * UID로 사용자 조회
     */
    public User getUserByUid(String uid) {
        return userRepository.findById(uid).orElse(null);
    }

    /**
     * 이메일로 사용자 조회 (OTP 등에서 사용)
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    /**
     * 이메일 중복 확인
     * 
     * @param email 확인할 이메일 주소
     * @return 중복 여부 (true: 이미 사용 중, false: 사용 가능)
     */
    public boolean isEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * 🔥 OTP 인증 성공 시 이메일 인증 완료로 업데이트
     */
    public void updateEmailVerified(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setEmailVerified(true);  // ← 반드시 User 엔티티에 emailVerified 필드 존재해야 함

        userRepository.save(user);
    }

    /**
     * 🔥 이메일 인증 여부 확인 API
     */
    public boolean isEmailVerified(String email) {
        return userRepository.findByEmail(email)
                .map(User::isEmailVerified)
                .orElse(false);
    }

    /**
     * 사용자 정보 업데이트
     */
    @Transactional
    public void updateUser(User user) {
        userRepository.save(user);
    }

    /**
     * 프로필 저장/업데이트
     */
    @Transactional
    public void saveOrUpdateProfile(String userId, ProfileRequest profileRequest) {
        // 1) User 조회 (UserController에서 이미 생성되었으므로 존재해야 함)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 2) 생년월일 파싱 (ProfileRequest는 String, UserProfile은 LocalDate)
        LocalDate birthDate = LocalDate.parse(profileRequest.getBirthDate(), DateTimeFormatter.ISO_LOCAL_DATE);

        // 3) UserProfile 저장/업데이트
        UserProfile profile = userProfileRepository.findByUser_UserId(userId)
                .orElse(UserProfile.builder().user(user).build());

        profile.setBirthYear(birthDate);
        profile.setNickname(profileRequest.getNickname());
        profile.setGender(profileRequest.getGender());
        // region은 VARCHAR(10)이므로 province만 저장 (예: "서울", "경기", "강원")
        // city는 별도로 저장하지 않음 (스키마 제약)
        String region = profileRequest.getProvince();
        if (region != null && region.length() > 10) {
            region = region.substring(0, 10); // 최대 10자로 제한
        }
        profile.setRegion(region);
        profile.setEducation(profileRequest.getEducation());
        profile.setJobStatus(profileRequest.getEmployment());

        userProfileRepository.save(profile);

        // 4) 기존 관심사 삭제 후 새로 저장
        List<InterestCategory> existingInterests = interestCategoryRepository.findByUser_UserId(userId);
        if (!existingInterests.isEmpty()) {
            interestCategoryRepository.deleteAll(existingInterests);
        }

        // 5) 새로운 관심사 저장
        if (profileRequest.getInterests() != null && !profileRequest.getInterests().isEmpty()) {
            List<InterestCategory> newInterests = profileRequest.getInterests().stream()
                    .map(interest -> InterestCategory.builder()
                            .user(user)
                            .category(interest)
                            .build())
                    .toList();
            interestCategoryRepository.saveAll(newInterests);
        }
    }

    /**
     * FCM 토큰 저장/업데이트
     */
    @Transactional
    public void saveFcmToken(String userId, String fcmToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        user.setPushToken(fcmToken);
        userRepository.save(user);
    }

    /**
     * 사용자 ID로 FCM 토큰 조회
     */
    public String getFcmTokenByUserId(String userId) {
        return userRepository.findById(userId)
                .map(User::getPushToken)
                .orElse(null);
    }

    /**
     * 사용자 ID로 프로필 조회
     */
    public UserProfile getProfileByUserId(String userId) {
        return userProfileRepository.findByUser_UserId(userId).orElse(null);
    }

    /**
     * 사용자 프로필을 UserProfileResponse로 변환
     */
    public UserProfileResponse getUserProfile(String userId) {
        UserProfile profile = userProfileRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new RuntimeException("프로필을 찾을 수 없습니다: " + userId));

        // 나이 계산 (생년월일 기준)
        Integer age = null;
        if (profile.getBirthYear() != null) {
            LocalDate birthDate = profile.getBirthYear();
            LocalDate today = LocalDate.now();
            age = (int) ChronoUnit.YEARS.between(birthDate, today);
        }

        // 관심사 목록 조회
        List<String> interests = interestCategoryRepository.findByUser_UserId(userId)
                .stream()
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
    }
}
