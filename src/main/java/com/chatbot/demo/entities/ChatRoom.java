package com.chatbot.demo.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity(name = "users")
@Data
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(name = "name")
    String name;
    @Column(name = "type")
    String type; // PUBLIC, PRIVATE, DIRECT
    @Column(name = "created_at")
    LocalDateTime createdAt;
}
