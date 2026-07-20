package com.gold.safefam.domain.user.service;

import com.gold.safefam.domain.user.dto.*;
import com.gold.safefam.domain.user.entity.User;
import com.gold.safefam.domain.user.repository.UserRepository;
import com.gold.safefam.global.exception.BusinessException;
import com.gold.safefam.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateMe(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.updateName(request.name());
        return UserResponse.from(user);
    }

    @Transactional
    public void withdraw(Long userId, WithdrawalRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (user.getKakaoId() == null) {
            if (!passwordEncoder.matches(request.password(), user.getPassword())) {
                throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
            }
        }
        user.delete();
    }

    @Transactional(readOnly = true)
    public UserSettingsResponse getSettings(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return new UserSettingsResponse(user.isAutoAnalysisEnabled(), user.isPushEnabled());
    }

    @Transactional
    public UserSettingsResponse updateSettings(Long userId, UpdateUserSettingsRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.updateSettings(request.autoAnalysisEnabled(), request.pushEnabled());
        return new UserSettingsResponse(user.isAutoAnalysisEnabled(), user.isPushEnabled());
    }
}