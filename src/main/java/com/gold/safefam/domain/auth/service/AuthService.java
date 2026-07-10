package com.gold.safefam.domain.auth.service;

import com.gold.safefam.domain.auth.dto.SignupRequest;
import com.gold.safefam.domain.user.entity.User;
import com.gold.safefam.domain.user.repository.UserRepository;
import com.gold.safefam.global.exception.BusinessException;
import com.gold.safefam.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 회원가입
    @Transactional
    public void signup(SignupRequest request) {
        String email = normalizeEmail(request.email());

        // email 중복 체크
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 비밀번호 BCrypt 암호화
        String encodedPassword = passwordEncoder.encode(request.password());
        User user = new User(email, encodedPassword, request.name());
        userRepository.save(user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
