package com.chatbot.demo.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Tạo Kafka topic khi ứng dụng khởi động (nếu chưa tồn tại).
 *
 * 3 partition để:
 * - Mỗi roomId được hash vào 1 partition cố định (ordering đảm bảo per room)
 * - 3 consumer thread trong group xử lý song song (khớp với listener.concurrency=3)
 */
@Configuration
public class KafkaTopicConfig {

    @Value("${app.kafka.chat-topic}")
    private String chatTopic;

    @Bean
    public NewTopic chatMessagesTopic() {
        return TopicBuilder.name(chatTopic)
                .partitions(3)
                .replicas(1) // 1 replica vì chỉ có 1 broker trong dev
                .build();
    }
}
