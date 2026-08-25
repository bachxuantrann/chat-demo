package com.chatbot.demo.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity(name = "messages")
@Data
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(name = "user_id")
    Long userId;
    @Column(name = "room_id")
    Long roomId;
    @Column(name = "sender_name")
    String senderName;
    @Column(name = "content", columnDefinition = "TEXT")
    String content;
    @Column(name = "message_type")
    String messageType; // CHAT, JOIN, LEAVE
    @Column(name = "created_at")
    LocalDateTime createdAt;
}
