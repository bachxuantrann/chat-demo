package com.chatbot.demo.service;

import com.chatbot.demo.dto.ChatMessageDto;
import com.chatbot.demo.dto.MessageHistoryResponse;
import com.chatbot.demo.entities.Message;
import com.chatbot.demo.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Xử lý việc lấy lịch sử tin nhắn với chiến lược đọc 2 tầng:
 *
 * Tầng 1 (Redis cache): 50 tin nhắn gần nhất, latency thấp (~1ms)
 * Tầng 2 (PostgreSQL):  Tin nhắn cũ hơn, phân trang, latency cao hơn (~10-50ms)
 *
 * Logic phân tầng:
 * - page=0 (mặc định): đọc từ Redis → trả về ngay nếu có cache
 * - page>0: đọc từ PostgreSQL với offset
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageHistoryService {

    private final RedisMessageService redisMessageService;
    private final MessageRepository messageRepository;

    private static final int PAGE_SIZE = 50;

    /**
     * Lấy history tin nhắn:
     * - page=0: ưu tiên Redis cache
     * - page>0: đọc từ DB với phân trang
     *
     * @param roomId ID phòng chat
     * @param page   Trang (0-indexed). page=0 là 50 tin mới nhất.
     */
    public MessageHistoryResponse getHistory(Long roomId, int page) {
        if (page == 0) {
            // Thử đọc từ Redis trước
            List<ChatMessageDto> cached = redisMessageService.getRecentMessages(roomId);
            if (!cached.isEmpty()) {
                log.debug("Cache HIT for roomId={}, {} messages", roomId, cached.size());
                return MessageHistoryResponse.builder()
                        .messages(cached)
                        .fromCache(true)
                        .build();
            }
            log.debug("Cache MISS for roomId={}, falling back to DB", roomId);
        }

        // Đọc từ PostgreSQL
        return getFromDatabase(roomId, page);
    }

    private MessageHistoryResponse getFromDatabase(Long roomId, int page) {
        Page<Message> dbPage = messageRepository.findByRoomIdOrderByCreatedAtDesc(
                roomId,
                PageRequest.of(page, PAGE_SIZE)
        );

        List<ChatMessageDto> messages = dbPage.getContent().stream()
                .map(this::toDto)
                // Reverse vì DB query sắp xếp DESC (mới nhất trước),
                // nhưng client cần hiển thị cũ → mới
                .collect(Collectors.toList());
        java.util.Collections.reverse(messages);

        log.debug("DB query for roomId={}, page={}, found={}", roomId, page, messages.size());

        return MessageHistoryResponse.builder()
                .messages(messages)
                .fromCache(false)
                .totalMessages(dbPage.getTotalElements())
                .build();
    }

    private ChatMessageDto toDto(Message message) {
        return ChatMessageDto.builder()
                .roomId(message.getRoomId())
                .senderId(message.getUserId())
                .senderName(message.getSenderName())
                .content(message.getContent())
                .messageType(ChatMessageDto.MessageType.valueOf(message.getMessageType()))
                .timestamp(message.getCreatedAt())
                .build();
    }
}
