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

    @Column(nullable = false, length = 30)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    public User(String phoneNumber, String password, String name) {
        this.phoneNumber = phoneNumber;
        this.password = password;
        this.name = name;
        this.role = UserRole.USER;
    }

    @Column(name = "kakao_id", unique = true, length = 100)
    private String kakaoId;

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
}
