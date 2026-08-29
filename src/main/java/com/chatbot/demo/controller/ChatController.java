package com.chatbot.demo.controller;

import com.chatbot.demo.dto.ChatMessageDto;
import com.chatbot.demo.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

/**
 * WebSocket Controller xử lý tin nhắn STOMP từ client.
 *
 * Flow:
 * Client STOMP SEND → /app/chat/{roomId}
 *   → @MessageMapping("/chat/{roomId}")
 *   → ChatService.handleMessage()
 *   → SimpMessagingTemplate broadcast → /topic/room/{roomId}
 *   → Tất cả subscriber nhận được tin
 *
 * Lưu ý: Không dùng @SendTo vì chúng ta muốn kiểm soát
 * broadcast trong ChatService (enrich timestamp trước khi gửi).
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * Nhận tin nhắn STOMP từ client và xử lý.
     *
     * Client gửi đến: /app/chat/{roomId}
     * Server broadcast đến: /topic/room/{roomId}
     */
    @MessageMapping("/chat/{roomId}")
    public void sendMessage(
            @DestinationVariable Long roomId,
            ChatMessageDto message) {

        message.setRoomId(roomId); // Đảm bảo roomId khớp với URL path
        log.debug("WS message received: room={}, sender={}", roomId, message.getSenderName());
        chatService.handleMessage(message);
    }

    /**
     * Xử lý sự kiện user join phòng.
     * Client gửi đến: /app/chat/{roomId}/join
     */
    @MessageMapping("/chat/{roomId}/join")
    public void joinRoom(
            @DestinationVariable Long roomId,
            ChatMessageDto message) {

        chatService.handleJoin(roomId, message.getSenderName());
    }

    /**
     * Xử lý sự kiện user rời phòng.
     * Client gửi đến: /app/chat/{roomId}/leave
     */
    @MessageMapping("/chat/{roomId}/leave")
    public void leaveRoom(
            @DestinationVariable Long roomId,
            ChatMessageDto message) {

        chatService.handleLeave(roomId, message.getSenderName());
    }
}
