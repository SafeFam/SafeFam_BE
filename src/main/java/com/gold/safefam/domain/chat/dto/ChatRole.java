package com.gold.safefam.domain.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "챗봇 대화 메시지 작성자")
public enum ChatRole {
    USER,
    ASSISTANT
}
