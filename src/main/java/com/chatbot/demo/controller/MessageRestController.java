package com.chatbot.demo.controller;

import com.chatbot.demo.dto.MessageHistoryResponse;
import com.chatbot.demo.service.MessageHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API cho việc lấy lịch sử tin nhắn.
 *
 * Chiến lược phân tầng:
 * - GET /api/rooms/{roomId}/messages          → page=0, ưu tiên Redis
 * - GET /api/rooms/{roomId}/messages?page=1   → page=1, đọc từ DB (51-100 tin cũ hơn)
 * - GET /api/rooms/{roomId}/messages?page=2   → page=2, đọc từ DB (101-150 tin cũ hơn)
 *
 * Client flow khi load phòng chat:
 * 1. Gọi API này (page=0) → nhận 50 tin gần nhất từ Redis
 * 2. Kết nối WebSocket để nhận tin realtime
 * 3. Khi user cuộn lên → gọi lại với page=1, 2, ...
 */
@Slf4j
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class MessageRestController {

    private final MessageHistoryService messageHistoryService;

    /**
     * Lấy lịch sử tin nhắn của một phòng.
     *
     * @param roomId ID phòng chat
     * @param page   Số trang (default=0). page=0 ưu tiên Redis cache.
     * @return List tin nhắn (cũ → mới) + metadata
     */
    @GetMapping("/{roomId}/messages")
    public ResponseEntity<MessageHistoryResponse> getMessages(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page) {

        log.debug("REST: get messages for roomId={}, page={}", roomId, page);
        MessageHistoryResponse response = messageHistoryService.getHistory(roomId, page);
        return ResponseEntity.ok(response);
    }
}
