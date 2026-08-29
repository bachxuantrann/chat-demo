package com.chatbot.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response cho REST API lấy lịch sử tin nhắn.
 * fromCache giúp client (và developer) biết data đến từ Redis hay PostgreSQL.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageHistoryResponse {

    private List<ChatMessageDto> messages;

    /** true = data từ Redis cache; false = data từ PostgreSQL */
    private boolean fromCache;

    /** Tổng số tin nhắn trong phòng (chỉ trả về khi query DB) */
    private Long totalMessages;
}
