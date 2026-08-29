package com.chatbot.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO truyền qua WebSocket, Redis và Kafka.
 * Dùng chung cho cả 3 luồng để đơn giản hóa serialize/deserialize.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {

    /** ID phòng chat — dùng làm Kafka partition key để đảm bảo ordering */
    private Long roomId;

    /** ID người gửi (nullable nếu hệ thống chưa có auth) */
    private Long senderId;

    /** Tên hiển thị của người gửi */
    private String senderName;

    /** Nội dung tin nhắn */
    private String content;

    /**
     * Loại tin nhắn:
     * - CHAT  : tin nhắn thường
     * - JOIN  : thông báo user vào phòng
     * - LEAVE : thông báo user rời phòng
     */
    private MessageType messageType;

    /** Thời điểm gửi — gán bởi server, không tin vào client */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    public enum MessageType {
        CHAT, JOIN, LEAVE
    }
}
