package com.example.youth.service;

import com.example.youth.DB.InterestCategory;
import com.example.youth.DB.User;
import com.example.youth.DB.UserProfile;
import com.example.youth.dto.ProfileRequest;
import com.example.youth.repository.InterestCategoryRepository;
import com.example.youth.repository.UserProfileRepository;
import com.example.youth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
        // 1) User 조회 (이미 UserController에서 생성되었으므로 존재해야 함)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 2) 생년월일 파싱 (ProfileRequest는 String, UserProfile은 LocalDate)
        LocalDate birthDate = LocalDate.parse(profileRequest.getBirthDate(), DateTimeFormatter.ISO_LOCAL_DATE);

        // 3) UserProfile 저장/업데이트
        UserProfile profile = userProfileRepository.findByUser_UserId(userId)
                .orElse(UserProfile.builder().user(user).build());

        profile.setBirthYear(birthDate);
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
}
