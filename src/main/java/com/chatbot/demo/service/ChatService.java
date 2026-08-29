package com.chatbot.demo.service;

import com.chatbot.demo.dto.ChatMessageDto;
import com.chatbot.demo.kafka.ChatMessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Core service xử lý tin nhắn chat.
 *
 * Luồng xử lý khi nhận tin nhắn:
 * 1. Gán timestamp (server-side, không tin client)
 * 2. Song song:
 *    a. Push vào Redis cache (50 tin gần nhất)
 *    b. Broadcast qua WebSocket tới tất cả subscriber của phòng
 *    c. Bắn event vào Kafka (async, không block)
 *
 * Thiết kế: Redis + WebSocket được ưu tiên thực hiện trước vì
 * đây là critical path (user cần thấy tin nhắn ngay lập tức).
 * Kafka/DB là async — có thể bị delay mà không ảnh hưởng UX.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final RedisMessageService redisMessageService;
    private final ChatMessageProducer kafkaProducer;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Xử lý tin nhắn chat từ WebSocket controller.
     *
     * @param message DTO nhận từ client (chưa có timestamp)
     * @return DTO đã được enrich với timestamp để gửi ngược lại client
     */
    public ChatMessageDto handleMessage(ChatMessageDto message) {
        // 1. Gán timestamp server-side
        message.setTimestamp(LocalDateTime.now());

        log.debug("Processing message from={} in room={}", message.getSenderName(), message.getRoomId());

        // 2a. Push vào Redis cache (tối đa 50 tin gần nhất)
        redisMessageService.pushMessage(message.getRoomId(), message);

        // 2b. Broadcast qua WebSocket tới tất cả client đang subscribe phòng này
        String destination = "/topic/room/" + message.getRoomId();
        messagingTemplate.convertAndSend(destination, message);
        log.debug("Broadcasted to WebSocket destination={}", destination);

        // 2c. Gửi event vào Kafka (async, non-blocking)
        // Key = roomId → Kafka đảm bảo ordering cho mỗi phòng
        kafkaProducer.sendMessage(message);

        return message;
    }

    /**
     * Xử lý sự kiện user join phòng.
     * Tạo system message thông báo và broadcast cho phòng.
     */
    public void handleJoin(Long roomId, String username) {
        ChatMessageDto joinMessage = ChatMessageDto.builder()
                .roomId(roomId)
                .senderName("System")
                .content(username + " đã tham gia phòng chat")
                .messageType(ChatMessageDto.MessageType.JOIN)
                .timestamp(LocalDateTime.now())
                .build();

        String destination = "/topic/room/" + roomId;
        messagingTemplate.convertAndSend(destination, joinMessage);
        log.info("User {} joined room {}", username, roomId);
    }

    /**
     * Xử lý sự kiện user leave phòng.
     */
    public void handleLeave(Long roomId, String username) {
        ChatMessageDto leaveMessage = ChatMessageDto.builder()
                .roomId(roomId)
                .senderName("System")
                .content(username + " đã rời khỏi phòng chat")
                .messageType(ChatMessageDto.MessageType.LEAVE)
                .timestamp(LocalDateTime.now())
                .build();

        String destination = "/topic/room/" + roomId;
        messagingTemplate.convertAndSend(destination, leaveMessage);
        log.info("User {} left room {}", username, roomId);
    }
}
