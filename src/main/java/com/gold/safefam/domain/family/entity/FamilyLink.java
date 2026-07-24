package com.gold.safefam.domain.family.entity;

import com.gold.safefam.domain.family.enums.FamilyLinkStatus;
import com.gold.safefam.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "family_links")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FamilyLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "protector_id", nullable = false)
    private User protector;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "protected_id")
    private User ward;

    @Column(name = "invite_code", unique = true, length = 6)
    private String inviteCode;

    @Column(name = "qr_token", unique = true)
    private String qrToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FamilyLinkStatus status = FamilyLinkStatus.PENDING;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "linked_at")
    private OffsetDateTime linkedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public static FamilyLink createInvite(User protector, String inviteCode, String qrToken, OffsetDateTime expiresAt) {
        FamilyLink link = new FamilyLink();
        link.protector = protector;
        link.inviteCode = inviteCode;
        link.qrToken = qrToken;
        link.status = FamilyLinkStatus.PENDING;
        link.expiresAt = expiresAt;
        link.createdAt = OffsetDateTime.now();
        return link;
    }

    public void accept(User ward) {
        this.ward = ward;
        this.status = FamilyLinkStatus.ACTIVE;
        this.linkedAt = OffsetDateTime.now();
    }

    public void revoke() {
        this.status = FamilyLinkStatus.REVOKED;
    }

    public boolean isExpired() {
        return expiresAt != null && OffsetDateTime.now().isAfter(expiresAt);
    }
}