package com.chatbot.demo.kafka;

import com.chatbot.demo.dto.ChatMessageDto;
import com.chatbot.demo.entities.Message;
import com.chatbot.demo.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Consumer lắng nghe Kafka topic và lưu tin nhắn vào PostgreSQL.
 *
 * Thiết kế at-least-once delivery:
 * - Manual acknowledgment (ack-mode: manual_immediate)
 * - Chỉ ACK sau khi insert DB thành công
 * - Nếu app crash trước khi ACK → Kafka gửi lại khi restart
 *
 * Idempotency note: Trong hệ thống production nên có mechanism
 * chống duplicate (ví dụ: unique constraint trên message ID).
 * Demo này chưa implement để giữ code đơn giản.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageConsumer {

    private final MessageRepository messageRepository;

    /**
     * Lắng nghe topic chat-messages, group chat-storage-group.
     * concurrency=3 → 3 instance của listener này chạy song song
     * (mỗi instance xử lý 1 partition).
     */
    @KafkaListener(
            topics = "${app.kafka.chat-topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, ChatMessageDto> record, Acknowledgment acknowledgment) {
        ChatMessageDto dto = record.value();
        log.debug("Received from Kafka: topic={}, partition={}, offset={}, roomId={}",
                record.topic(), record.partition(), record.offset(), dto.getRoomId());

        try {
            Message entity = toEntity(dto);
            messageRepository.save(entity);
            log.debug("Saved message to DB: roomId={}, sender={}", dto.getRoomId(), dto.getSenderName());

            // ACK sau khi insert thành công → commit offset
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to save message to DB: roomId={}, error={}", dto.getRoomId(), e.getMessage());
            // Không ACK → Kafka sẽ gửi lại message này khi consumer restart
            // Production nên implement Dead Letter Queue (DLQ) để tránh poison pill
        }
    }

    private Message toEntity(ChatMessageDto dto) {
        Message message = new Message();
        message.setRoomId(dto.getRoomId());
        message.setUserId(dto.getSenderId());
        message.setSenderName(dto.getSenderName());
        message.setContent(dto.getContent());
        message.setMessageType(dto.getMessageType().name());
        // Dùng timestamp từ producer (server-side), không phải thời điểm consumer nhận
        message.setCreatedAt(dto.getTimestamp() != null ? dto.getTimestamp() : LocalDateTime.now());
        return message;
    }
}
