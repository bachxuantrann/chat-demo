package com.chatbot.demo.entities;


import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Entity(name = "users")
@Data
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(name = "username", nullable = false, unique = true)
    String username;
    @Column(name = "full_name")
    String fullName;
}
