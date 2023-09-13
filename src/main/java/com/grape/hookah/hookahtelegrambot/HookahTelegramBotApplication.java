package com.grape.hookah.hookahtelegrambot;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.grape.hookah.hookahtelegrambot.bots",
        "org.telegram.telegrambots",
        "com.grape"
})
@RegisterReflectionForBinding({
        org.telegram.telegrambots.meta.api.objects.ApiResponse.class,
        org.telegram.telegrambots.updatesreceivers.DefaultBotSession.class,
        org.telegram.telegrambots.meta.api.methods.send.SendMessage.class
})
public class HookahTelegramBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(HookahTelegramBotApplication.class, args);
    }
}
