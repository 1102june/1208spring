package com.example.youth.service;

import com.example.youth.DB.InterestCategory;
import com.example.youth.DB.User;
import com.example.youth.DB.UserProfile;
import com.example.youth.dto.ProfileRequest;
import com.example.youth.dto.ProfileSaveResponse;
import com.example.youth.dto.UserProfileResponse;
import com.example.youth.repository.InterestCategoryRepository;
import com.example.youth.repository.UserProfileRepository;
import com.example.youth.repository.UserRepository;
import com.example.youth.repository.BookmarkRepository;
import com.example.youth.repository.CalendarEventRepository;
import com.example.youth.repository.UserActivityRepository;
import com.example.youth.repository.NotificationRepository;
import com.example.youth.repository.AIRecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final BookmarkRepository bookmarkRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final UserActivityRepository userActivityRepository;
    private final NotificationRepository notificationRepository;
    private final AIRecommendationRepository aiRecommendationRepository;
    private final UserPolicyRecommendationService userPolicyRecommendationService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

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
     * 프로필 저장/업데이트. 저장 직후 해당 사용자 Top-K 추천 캐시를 동기 갱신한다.
     */
    @Transactional
    public ProfileSaveResponse saveOrUpdateProfile(String userId, ProfileRequest profileRequest) {
        // 1) User 조회 (UserController에서 이미 생성되었으므로 존재해야 함)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 2) User 정보 업데이트 (appVersion, deviceId, pushToken)
        if (profileRequest.getAppVersion() != null && !profileRequest.getAppVersion().isEmpty()) {
            user.setAppVersion(profileRequest.getAppVersion());
        }
        if (profileRequest.getDeviceId() != null && !profileRequest.getDeviceId().isEmpty()) {
            user.setDeviceId(profileRequest.getDeviceId());
        }
        if (profileRequest.getPushToken() != null && !profileRequest.getPushToken().isEmpty()) {
            user.setPushToken(profileRequest.getPushToken());
        }
        userRepository.save(user);

        // 3) UserProfile 저장/업데이트 (중복 시 가장 최신 것 사용)
        // 중복 방지를 위해 List로 받아서 첫 번째만 사용
        List<UserProfile> profiles = userProfileRepository.findAllByUser_UserId(userId);
        UserProfile profile = profiles.isEmpty() 
                ? UserProfile.builder().user(user).build()
                : profiles.get(0); // 가장 최신 것 (ORDER BY profile_id DESC)

        // 4) 생년월일 파싱 및 설정 (ProfileRequest는 String, UserProfile은 LocalDate)
        if (profileRequest.getBirthDate() != null && !profileRequest.getBirthDate().isEmpty()) {
            try {
                LocalDate birthDate = LocalDate.parse(profileRequest.getBirthDate(), DateTimeFormatter.ISO_LOCAL_DATE);
                profile.setBirthYear(birthDate);
            } catch (Exception e) {
                System.err.println("생년월일 파싱 실패: " + e.getMessage());
            }
        }
        
        // 5) 닉네임, 지역, 직업, 교육 정보 업데이트 (birthDate와 무관하게 항상 업데이트)
        if (profileRequest.getNickname() != null) {
            profile.setNickname(profileRequest.getNickname());
        }
        if (profileRequest.getGender() != null) {
            profile.setGender(profileRequest.getGender());
        }
        // region은 province와 city를 합쳐서 저장 (최대 50자까지 허용)
        // "경기도 수원시" 형식으로 저장
        String region = null;
        if (profileRequest.getProvince() != null && !profileRequest.getProvince().isEmpty()) {
            if (profileRequest.getCity() != null && !profileRequest.getCity().isEmpty()) {
                region = profileRequest.getProvince() + " " + profileRequest.getCity();
            } else {
                region = profileRequest.getProvince();
            }
            // VARCHAR(50) 제한 확인 (스키마에 따라 조정 필요)
            if (region.length() > 50) {
                region = region.substring(0, 50); // 최대 50자로 제한
            }
            profile.setRegion(region);
        }
        if (profileRequest.getEducation() != null) {
            profile.setEducation(profileRequest.getEducation());
        }
        if (profileRequest.getEmployment() != null) {
            profile.setJobStatus(profileRequest.getEmployment());
        }

        userProfileRepository.save(profile);

        // 5) 기존 관심사 삭제 후 새로 저장
        List<InterestCategory> existingInterests = interestCategoryRepository.findByUser_UserId(userId);
        if (!existingInterests.isEmpty()) {
            interestCategoryRepository.deleteAll(existingInterests);
        }

        // 6) 새로운 관심사 저장 (interests 또는 category 사용)
        List<String> interestsList = profileRequest.getInterestsList();
        if (!interestsList.isEmpty()) {
            List<InterestCategory> newInterests = interestsList.stream()
                    .map(interest -> InterestCategory.builder()
                            .user(user)
                            .category(interest)
                            .build())
                    .toList();
            interestCategoryRepository.saveAll(newInterests);
        }

        // 7) 추천 캐시(Top-K) 해당 사용자 기준 즉시 갱신 (프로필 저장과 동일 트랜잭션 커밋 후 반영)
        int recommendationCount = userPolicyRecommendationService.recomputeForUser(userId);
        return ProfileSaveResponse.builder()
                .recommendationsRefreshed(true)
                .recommendationCount(recommendationCount)
                .build();
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
        // 중복 방지를 위해 List로 받아서 첫 번째만 사용
        List<UserProfile> profiles = userProfileRepository.findAllByUser_UserId(userId);
        return profiles.isEmpty() ? null : profiles.get(0);
    }

    /**
     * 사용자 프로필을 UserProfileResponse로 변환
     * 프로필이 없으면 null 반환 (예외 던지지 않음)
     */
    public UserProfileResponse getUserProfile(String userId) {
        try {
            // 중복 방지를 위해 List로 받아서 첫 번째만 사용
            List<UserProfile> profiles = userProfileRepository.findAllByUser_UserId(userId);
            if (profiles.isEmpty()) {
                return null;
            }
            
            UserProfile profile = profiles.get(0); // 가장 최신 것 (ORDER BY profile_id DESC)

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
        } catch (Exception e) {
            // 프로필 조회 실패 시 null 반환 (예외를 던지지 않음)
            System.err.println("프로필 조회 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 비밀번호 해시화 (BCrypt)
     */
    public String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            return "";
        }
        return passwordEncoder.encode(password);
    }

    /**
     * 회원탈퇴
     * 사용자와 관련된 모든 데이터를 삭제합니다.
     * 
     * @param userId 사용자 ID
     * @throws RuntimeException 사용자를 찾을 수 없는 경우
     */
    @Transactional
    public void deleteUser(String userId) {
        // 1) 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 2) 자식 테이블부터 삭제 (외래키 제약조건 고려)
        // InterestCategory 삭제
        List<InterestCategory> interests = interestCategoryRepository.findByUser_UserId(userId);
        if (!interests.isEmpty()) {
            interestCategoryRepository.deleteAll(interests);
        }

        // Bookmark 삭제
        List<com.example.youth.DB.Bookmark> bookmarks = bookmarkRepository.findByUser(user);
        if (!bookmarks.isEmpty()) {
            bookmarkRepository.deleteAll(bookmarks);
        }

        // CalendarEvent 삭제
        List<com.example.youth.DB.CalendarEvent> calendarEvents = calendarEventRepository.findByUser(user);
        if (!calendarEvents.isEmpty()) {
            calendarEventRepository.deleteAll(calendarEvents);
        }

        // UserActivity 삭제
        List<com.example.youth.DB.UserActivity> userActivities = userActivityRepository.findByUser_UserIdOrderByCreatedAtDesc(userId);
        if (!userActivities.isEmpty()) {
            userActivityRepository.deleteAll(userActivities);
        }

        // Notification 삭제
        List<com.example.youth.DB.Notification> notifications = notificationRepository.findByUser_UserIdOrderBySendDateDesc(userId);
        if (!notifications.isEmpty()) {
            notificationRepository.deleteAll(notifications);
        }

        // AIRecommendation 삭제
        List<com.example.youth.DB.AIRecommendation> aiRecommendations = aiRecommendationRepository.findByUser_UserIdOrderByCreatedAtDesc(userId);
        if (!aiRecommendations.isEmpty()) {
            aiRecommendationRepository.deleteAll(aiRecommendations);
        }

        // UserProfile 삭제
        List<UserProfile> profiles = userProfileRepository.findAllByUser_UserId(userId);
        if (!profiles.isEmpty()) {
            userProfileRepository.deleteAll(profiles);
        }

        // 3) User 삭제 (마지막)
        userRepository.delete(user);
    }
}
