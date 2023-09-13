package com.grape.hookah.hookahtelegrambot.serivce;

import com.grape.hookah.hookahtelegrambot.dao.HookahRateRepository;
import com.grape.hookah.hookahtelegrambot.dao.HookahUserRepository;
import com.grape.hookah.hookahtelegrambot.model.HookahRate;
import com.grape.hookah.hookahtelegrambot.model.HookahUser;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class HookahRateService {

    private final HookahUserRepository hookahUserRepository;
    private final HookahRateRepository hookahRateRepository;

    public HookahRateService(HookahUserRepository hookahUserRepository, HookahRateRepository hookahRateRepository) {
        this.hookahUserRepository = hookahUserRepository;
        this.hookahRateRepository = hookahRateRepository;
    }

    @Transactional
    public void saveHookahRateAndStartTracking(HookahRate hookahRate, HookahUser hookahUser) {
        HookahRate savedHookahRate = hookahRateRepository.save(hookahRate);
        hookahUserRepository.updateHookahUserLastRatedHookahByChatId(hookahUser.getChatId(), savedHookahRate);
    }
}
