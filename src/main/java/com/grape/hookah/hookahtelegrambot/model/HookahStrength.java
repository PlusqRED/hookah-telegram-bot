package com.grape.hookah.hookahtelegrambot.model;

import lombok.Getter;

@Getter
public enum HookahStrength {
    LIGHT("Для девчонок"), BELOW_MEDIUM("Для пацанок"), MEDIUM("Ну норм"), ABOVE_MEDIUM("Хороший"), STRONG("Улетел в космос");

    private final String strength;

    HookahStrength(String strength) {
        this.strength = strength;
    }
}
