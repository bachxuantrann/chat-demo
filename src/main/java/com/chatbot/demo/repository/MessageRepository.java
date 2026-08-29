package com.chatbot.demo.repository;

import com.chatbot.demo.entities.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Lấy tin nhắn theo phòng, sắp xếp mới nhất trước.
     * Dùng cho phân trang khi client cuộn lên xem tin nhắn cũ (page > 0).
     */
    Page<Message> findByRoomIdOrderByCreatedAtDesc(Long roomId, Pageable pageable);

    /** Đếm tổng số tin nhắn trong phòng */
    long countByRoomId(Long roomId);
}
