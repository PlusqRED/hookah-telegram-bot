package com.grape.hookah.hookahtelegrambot.dao;

import com.grape.hookah.hookahtelegrambot.model.HookahRate;
import com.grape.hookah.hookahtelegrambot.model.HookahUser;
import com.grape.hookah.hookahtelegrambot.model.HookahUserState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface HookahUserRepository extends JpaRepository<HookahUser, Long> {
    @Transactional
    Optional<HookahUser> findHookahUserByChatId(Long chatId);

    @Modifying
    @Query("update HookahUser u set u.state = ?2 where u.chatId = ?1")
    @Transactional
    void updateHookahUserStateByChatId(Long chatId, HookahUserState hookahUserState);

    @Modifying
    @Query("update HookahUser u set u.lastRatedHookah = ?2 where u.chatId = ?1")
    @Transactional
    void updateHookahUserLastRatedHookahByChatId(Long chatId, HookahRate hookahRate);
}
