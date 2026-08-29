package com.chatbot.demo.kafka;

import com.chatbot.demo.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Producer gửi tin nhắn chat vào Kafka topic.
 *
 * Key = roomId (String) → Kafka hash key vào partition
 * → Tất cả tin nhắn cùng phòng đi vào cùng partition
 * → Đảm bảo thứ tự tin nhắn trong mỗi phòng
 *
 * Gửi async (non-blocking) với callback để log kết quả.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageProducer {

    private final KafkaTemplate<String, ChatMessageDto> kafkaTemplate;

    @Value("${app.kafka.chat-topic}")
    private String chatTopic;

    /**
     * Gửi ChatMessageDto vào Kafka với key = roomId.
     * Không block thread gọi — sử dụng CompletableFuture callback.
     */
    public void sendMessage(ChatMessageDto message) {
        String key = String.valueOf(message.getRoomId());

        CompletableFuture<SendResult<String, ChatMessageDto>> future =
                kafkaTemplate.send(chatTopic, key, message);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("Kafka send OK: topic={}, partition={}, offset={}, roomId={}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        message.getRoomId());
            } else {
                // Log error nhưng không crash — message vẫn đã được push Redis và broadcast WS
                log.error("Kafka send FAILED for roomId={}: {}", message.getRoomId(), ex.getMessage());
            }
        });
    }
}
