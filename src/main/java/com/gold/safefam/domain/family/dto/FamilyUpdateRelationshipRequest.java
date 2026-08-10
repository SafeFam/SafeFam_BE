package com.gold.safefam.domain.family.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "관계 설정 요청")
public record FamilyUpdateRelationshipRequest(
        @Size(max = 20)
        String relationship
) {}