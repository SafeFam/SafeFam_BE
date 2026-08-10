package com.gold.safefam.domain.user.entity;

import com.gold.safefam.domain.user.enums.UserRole;
import com.gold.safefam.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number", unique = true, length = 20)
    private String phoneNumber;

    @Column(name = "password_hash")
    private String password;

    @Column(name = "kakao_id", unique = true, length = 100)
    private String kakaoId;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(name = "nickname", length = 50)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(name = "login_fail_count", nullable = false)
    private int loginFailCount = 0;

    @Column(name = "is_locked", nullable = false)
    private boolean isLocked = false;

    @Column(name = "auto_analysis_enabled", nullable = false)
    private boolean autoAnalysisEnabled = true;

    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled = true;

    public User(String phoneNumber, String password, String name) {
        this.phoneNumber = phoneNumber;
        this.password = password;
        this.name = name;
        this.role = UserRole.USER;
    }

    public static User ofKakao(String kakaoId, String phoneNumber, String name) {
        User user = new User();
        user.kakaoId = kakaoId;
        user.phoneNumber = phoneNumber;
        user.name = name;
        user.role = UserRole.USER;
        return user;
    }

    public void linkKakao(String kakaoId) {
        this.kakaoId = kakaoId;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updatePassword(String password) {
        this.password = password;
    }

    public void updateSettings(Boolean autoAnalysisEnabled, Boolean pushEnabled) {
        if (autoAnalysisEnabled != null) this.autoAnalysisEnabled = autoAnalysisEnabled;
        if (pushEnabled != null) this.pushEnabled = pushEnabled;
    }

    public void incrementLoginFailCount() {
        this.loginFailCount++;
        if (this.loginFailCount >= 5) {
            this.isLocked = true;
        }
    }

    public void resetLoginFail() {
        this.loginFailCount = 0;
        this.isLocked = false;
    }
}
