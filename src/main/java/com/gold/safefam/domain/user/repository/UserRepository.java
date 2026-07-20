package com.gold.safefam.domain.user.repository;

import com.gold.safefam.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByPhoneNumber(String phoneNumber);

    Optional<User> findByPhoneNumber(String phoneNumber);
    Optional<User> findByKakaoId(String kakaoId);

    @Modifying
    @Query("UPDATE User u SET u.loginFailCount = u.loginFailCount + 1, u.isLocked = CASE WHEN u.loginFailCount + 1 >= 5 THEN true ELSE false END WHERE u.id = :userId")
    void incrementLoginFailCount(@Param("userId") Long userId);
}
