package com.gold.safefam.domain.family.service;

import com.gold.safefam.domain.family.entity.FamilyLink;
import com.gold.safefam.domain.family.enums.FamilyLinkStatus;
import com.gold.safefam.domain.family.repository.FamilyLinkRepository;
import com.gold.safefam.domain.user.entity.User;
import com.gold.safefam.domain.user.repository.UserRepository;
import com.gold.safefam.global.exception.BusinessException;
import com.gold.safefam.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FamilyServiceTest {

    @Mock private FamilyLinkRepository familyLinkRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private FamilyService familyService;

    private User guardian;
    private User ward;

    @BeforeEach
    void setUp() {
        guardian = new User("01011112222", "encoded-password", "보호자");
        ward = new User("01033334444", "encoded-password", "피보호자");

        // reflection으로 id 주입
        setId(guardian, 1L);
        setId(ward, 2L);
    }

    @Test
    void expiredInviteCode_throwsFamilyInviteExpired() {
        FamilyLink expiredLink = FamilyLink.createInvite(
                guardian, "999999", "qr-token",
                OffsetDateTime.now().minusMinutes(1)
        );

        given(familyLinkRepository.findByInviteCodeAndStatus("999999", FamilyLinkStatus.PENDING))
                .willReturn(Optional.of(expiredLink));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> familyService.linkByCode(2L, "999999"));

        assertEquals(ErrorCode.FAMILY_INVITE_EXPIRED, ex.getErrorCode());
    }

    @Test
    void selfLink_throwsFamilySelfLink() {
        FamilyLink link = FamilyLink.createInvite(
                guardian, "123456", "qr-token",
                OffsetDateTime.now().plusMinutes(10)
        );

        given(familyLinkRepository.findByInviteCodeAndStatus("123456", FamilyLinkStatus.PENDING))
                .willReturn(Optional.of(link));

        // 보호자가 본인 코드로 수락 시도
        BusinessException ex = assertThrows(BusinessException.class,
                () -> familyService.linkByCode(1L, "123456"));

        assertEquals(ErrorCode.FAMILY_SELF_LINK, ex.getErrorCode());
    }

    @Test
    void invalidInviteCode_throwsFamilyInviteNotFound() {
        given(familyLinkRepository.findByInviteCodeAndStatus("000000", FamilyLinkStatus.PENDING))
                .willReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> familyService.linkByCode(2L, "000000"));

        assertEquals(ErrorCode.FAMILY_INVITE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void unauthorizedRevoke_throwsFamilyLinkForbidden() {
        FamilyLink link = FamilyLink.createInvite(
                guardian, "123456", "qr-token",
                OffsetDateTime.now().plusMinutes(10)
        );
        link.accept(ward);
        setId(link, 10L);

        given(familyLinkRepository.findById(10L)).willReturn(Optional.of(link));

        // 제3자가 해제 시도
        BusinessException ex = assertThrows(BusinessException.class,
                () -> familyService.revoke(99L, 10L));

        assertEquals(ErrorCode.FAMILY_LINK_FORBIDDEN, ex.getErrorCode());
    }

    @Test
    void validateGuardianAccess_noRelation_throwsFamilyLinkForbidden() {
        given(familyLinkRepository.existsByProtectorIdAndWardIdAndStatus(1L, 2L, FamilyLinkStatus.ACTIVE))
                .willReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> familyService.validateGuardianAccess(1L, 2L));

        assertEquals(ErrorCode.FAMILY_LINK_FORBIDDEN, ex.getErrorCode());
    }

    private void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
