package com.grape.hookah.hookahtelegrambot.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HookahUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long chatId;

    private String name;

    @Enumerated(EnumType.ORDINAL)
    private HookahUserState state;

    @OneToOne
    @JoinColumn(name = "last_rated_hookah_id")
    private HookahRate lastRatedHookah;

    private LocalDateTime registeredDate;
}
