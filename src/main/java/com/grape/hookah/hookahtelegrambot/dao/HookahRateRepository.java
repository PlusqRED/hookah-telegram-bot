package com.grape.hookah.hookahtelegrambot.dao;

import com.grape.hookah.hookahtelegrambot.model.HookahRate;
import com.grape.hookah.hookahtelegrambot.model.HookahStrength;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface HookahRateRepository extends JpaRepository<HookahRate, Long> {

    @Modifying
    @Query("update HookahRate u set u.rate = ?2 where u.id = ?1")
    @Transactional
    void updateHookahRateById(Long id, Short parsedRate);

    @Modifying
    @Query("update HookahRate u set u.place = ?2 where u.id = ?1")
    @Transactional
    void updateHookahPlaceById(Long id, String place);

    @Modifying
    @Query("update HookahRate u set u.strength = ?2 where u.id = ?1")
    @Transactional
    void updateHookahStrengthById(Long id, HookahStrength strength);

    List<HookahRate> findAllByHookahUserId(Long id);
}
