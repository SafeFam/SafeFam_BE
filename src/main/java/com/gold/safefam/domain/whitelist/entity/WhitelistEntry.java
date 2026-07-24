package com.gold.safefam.domain.whitelist.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/** 인증 사용자가 분석에서 제외할 신뢰 발신자 한 건을 저장한다. */
@Getter
@Entity
@Table(name = "whitelist")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WhitelistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String sender;

    @Column(length = 50)
    private String label;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** 사용자 소유권과 정규화된 발신자 정보를 묶어 새 항목을 만든다. */
    public WhitelistEntry(Long userId, String sender, String label) {
        this.userId = userId;
        this.sender = sender;
        this.label = label;
    }
}
