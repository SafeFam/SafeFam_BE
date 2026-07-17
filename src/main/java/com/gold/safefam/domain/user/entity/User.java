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

    @Column(name = "phone_number", nullable = false, unique = true, length = 20)
    private String phoneNumber;

    @Column(name = "password_hash", nullable = false)
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

    public void updateName(String name) {
        this.name = name;
    }
}
