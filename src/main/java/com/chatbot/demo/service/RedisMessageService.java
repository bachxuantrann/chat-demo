package com.chatbot.demo.service;

import com.chatbot.demo.dto.ChatMessageDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Quản lý cache tin nhắn gần đây bằng Redis List.
 *
 * Redis key pattern: "room:messages:{roomId}"
 * Cấu trúc: Redis List (ordered), index 0 = tin mới nhất
 *
 * NOTE: GenericJackson2JsonRedisSerializer deserialize ra LinkedHashMap,
 * dùng ObjectMapper.convertValue() để convert sang ChatMessageDto.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisMessageService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.redis.max-recent-messages}")
    private int maxRecentMessages;

    private static final String KEY_PREFIX = "room:messages:";

    // ObjectMapper riêng với JavaTimeModule để handle LocalDateTime
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private String buildKey(Long roomId) {
        return KEY_PREFIX + roomId;
    }

    /**
     * Push tin nhắn mới vào đầu list Redis, sau đó trim giữ tối đa maxRecentMessages.
     */
    public void pushMessage(Long roomId, ChatMessageDto message) {
        String key = buildKey(roomId);
        try {
            redisTemplate.opsForList().leftPush(key, message);
            redisTemplate.opsForList().trim(key, 0, maxRecentMessages - 1);
            log.debug("Pushed message to Redis key={}", key);
        } catch (Exception e) {
            log.error("Failed to push message to Redis key={}: {}", key, e.getMessage());
        }
    }

    /**
     * Lấy toàn bộ tin nhắn gần nhất từ Redis.
     * Return list theo thứ tự cũ -> mới (reverse của Redis storage).
     *
     * GenericJackson2JsonRedisSerializer trả về LinkedHashMap ->
     * dùng ObjectMapper.convertValue() để convert an toàn sang ChatMessageDto.
     */
    public List<ChatMessageDto> getRecentMessages(Long roomId) {
        String key = buildKey(roomId);
        try {
            List<Object> raw = redisTemplate.opsForList().range(key, 0, -1);
            if (raw == null || raw.isEmpty()) {
                return Collections.emptyList();
            }
            List<ChatMessageDto> messages = raw.stream()
                    .filter(Objects::nonNull)
                    .map(obj -> objectMapper.convertValue(obj, ChatMessageDto.class))
                    .collect(Collectors.toList());
            Collections.reverse(messages);
            log.debug("Redis LRANGE key={}, found {} messages", key, messages.size());
            return messages;
        } catch (Exception e) {
            log.error("Failed to get messages from Redis key={}: {}", key, e.getMessage());
            return Collections.emptyList();
        }
    }

    public boolean hasCache(Long roomId) {
        String key = buildKey(roomId);
        Long size = redisTemplate.opsForList().size(key);
        return size != null && size > 0;
    }
}
