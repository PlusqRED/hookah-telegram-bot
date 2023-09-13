package com.grape.hookah.hookahtelegrambot.bots;

import com.grape.hookah.hookahtelegrambot.dao.HookahRateRepository;
import com.grape.hookah.hookahtelegrambot.dao.HookahUserRepository;
import com.grape.hookah.hookahtelegrambot.model.HookahRate;
import com.grape.hookah.hookahtelegrambot.model.HookahStrength;
import com.grape.hookah.hookahtelegrambot.model.HookahUser;
import com.grape.hookah.hookahtelegrambot.model.HookahUserState;
import com.grape.hookah.hookahtelegrambot.serivce.HookahRateService;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class HookahBot extends TelegramLongPollingBot {

    private final HookahUserRepository hookahUserRepository;
    private final HookahRateRepository hookahRateRepository;
    private final HookahRateService hookahRateService;
    private final Logger logger = LoggerFactory.getLogger(HookahBot.class);

    public HookahBot(@Value("${bot.token}") String botToken, HookahUserRepository hookahUserRepository, HookahRateRepository hookahRateRepository, HookahRateService hookahRateService) {
        super(botToken);
        this.hookahUserRepository = hookahUserRepository;
        this.hookahRateRepository = hookahRateRepository;
        this.hookahRateService = hookahRateService;
    }

    @Override
    @SneakyThrows
    public void onUpdateReceived(Update update) {
        if (update != null && update.getMessage() != null && update.getMessage().getText() != null && update.getMessage().getChatId() != null) {
            var message = update.getMessage();
            var chatId = message.getChatId();
            var hookahUser = meetAndGetUser(update, message, chatId);
            boolean resolved = resolveInitialMenu(update, hookahUser);
            if (!resolved) {
                resolveRateRequest(update, hookahUser);
            }
        }
    }

    @SneakyThrows
    private boolean resolveInitialMenu(Update update, HookahUser hookahUser) {
        if (update.getMessage().getText().equalsIgnoreCase("покажи мои оценки")) {
            List<HookahRate> allHookahsRatedByUser = hookahRateRepository.findAllByHookahUserId(hookahUser.getId());
            if (allHookahsRatedByUser.isEmpty()) {
                execute(SendMessage.builder().chatId(hookahUser.getChatId()).text("База данных дуделок пока пуста.").build());
            } else {
                String allRates = allHookahsRatedByUser.stream()
                        .sorted(Comparator.comparing(HookahRate::getRate))
                        .map(this::convertHokahRateToString)
                        .collect(Collectors.joining("\n----------------------------------"));

                execute(SendMessage.builder().chatId(hookahUser.getChatId()).text(allRates).build());
            }
            return true;
        } else {
            return false;
        }
    }

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String convertHokahRateToString(HookahRate hookahRate) {
        return "\nВкус: " + hookahRate.getFlavor() +
                "\nКрепость: " + hookahRate.getStrength().getStrength() +
                "\nОценка: " + hookahRate.getRate() +
                "/10\nМесто: " + hookahRate.getPlace() +
                "\nДата: " + hookahRate.getCreatedAt().format(dateTimeFormatter);
    }

    @SneakyThrows
    private void resolveRateRequest(Update update, HookahUser hookahUser) {
        HookahUserState currentUserState = hookahUser.getState();
        final Long chatId = hookahUser.getChatId();
        final HookahRate lastRatedHookah = hookahUser.getLastRatedHookah();
        switch (currentUserState) {
            case START -> hookahUserRepository.updateHookahUserStateByChatId(chatId, HookahUserState.FLAVOR);
            case FLAVOR -> {
                execute(SendMessage.builder()
                        .chatId(chatId)
                        .text("Введите вкусы кальяна. (Пример: яблоко, мармеладные мишки, чуть холодка)")
                        .replyMarkup(new ReplyKeyboardRemove(true))
                        .build()
                );

                hookahUserRepository.updateHookahUserStateByChatId(chatId, HookahUserState.STRENGTH);
            }
            case STRENGTH -> {
                String flavor = update.getMessage().getText();
                HookahRate hookahRate = HookahRate.builder()
                        .hookahUser(hookahUser)
                        .flavor(flavor)
                        .createdAt(LocalDateTime.now())
                        .build();
                hookahRateService.saveHookahRateAndStartTracking(hookahRate, hookahUser);

                execute(sendStrengthKeyboard(chatId));
                hookahUserRepository.updateHookahUserStateByChatId(chatId, HookahUserState.RATE);
            }
            case RATE -> {
                String strength = update.getMessage().getText();
                Optional<HookahStrength> maybeStrength = Arrays.stream(HookahStrength.values())
                        .filter(s -> strength.equalsIgnoreCase(s.getStrength()))
                        .findFirst();
                if (maybeStrength.isPresent()) {
                    hookahRateRepository.updateHookahStrengthById(lastRatedHookah.getId(), maybeStrength.get());

                    execute(sendRateKeyboard(chatId));
                    hookahUserRepository.updateHookahUserStateByChatId(chatId, HookahUserState.PLACE);
                } else {
                    execute(SendMessage.builder().chatId(chatId).text("Чел, введи то что просят, зачем ты пытаешься положить бота? Воспользуйся встроенной клавиатурой.").build());
                    execute(sendStrengthKeyboard(chatId));
                    hookahUserRepository.updateHookahUserStateByChatId(chatId, HookahUserState.RATE);
                }
            }
            case PLACE -> {
                try {
                    String rate = update.getMessage().getText();
                    short parsedRate = Short.parseShort(rate);
                    if (parsedRate < 0 || parsedRate > 10)
                        throw new NumberFormatException("Rate must be in [0, 10] range!");
                    hookahRateRepository.updateHookahRateById(lastRatedHookah.getId(), parsedRate);

                    execute(sendPlaceKeyboard(chatId));
                    hookahUserRepository.updateHookahUserStateByChatId(chatId, HookahUserState.FINISH);
                } catch (NumberFormatException ex) {
                    execute(SendMessage.builder().chatId(chatId).text("Да ёмае, выбери цифру нормальную. Воспользуйся встроенной клавиатурой.").build());
                    execute(sendRateKeyboard(chatId));
                    hookahUserRepository.updateHookahUserStateByChatId(chatId, HookahUserState.PLACE);
                }
            }
            case FINISH -> {
                String place = update.getMessage().getText();
                hookahRateRepository.updateHookahPlaceById(lastRatedHookah.getId(), place);

                String finishMessage = "Харош, учёл этот кальян на будущее!\nВкус: " + lastRatedHookah.getFlavor() + "\nКрепость: " + lastRatedHookah.getStrength().getStrength() + "\nОценка: " + lastRatedHookah.getRate() + "/10\nМесто: " + place;
                execute(SendMessage.builder()
                        .chatId(chatId)
                        .text(finishMessage)
                        .build()
                );

                execute(sendInitialKeyboard(chatId, "Оценим ещё?"));
                hookahUserRepository.updateHookahUserStateByChatId(chatId, HookahUserState.FLAVOR);
            }
        }
    }

    private SendMessage sendStrengthKeyboard(Long chatId) {
        KeyboardButton light = KeyboardButton.builder().text("Для девчонок").build();
        KeyboardButton belowMedium = KeyboardButton.builder().text("Для пацанок").build();
        KeyboardButton medium = KeyboardButton.builder().text("Ну норм").build();
        KeyboardButton aboveMedium = KeyboardButton.builder().text("Хороший").build();
        KeyboardButton strong = KeyboardButton.builder().text("Улетел в космос").build();
        ReplyKeyboardMarkup choice = ReplyKeyboardMarkup.builder()
                .keyboardRow(new KeyboardRow(List.of(light, belowMedium)))
                .keyboardRow(new KeyboardRow(List.of(medium, aboveMedium, strong)))
                .build();
        return SendMessage.builder().chatId(chatId).text("Выберите крепость кальяна:").replyMarkup(choice).build();
    }

    public SendMessage sendPlaceKeyboard(Long chatId) {
        KeyboardButton rateHookahButton = KeyboardButton.builder().text("Неважно").build();
        ReplyKeyboardMarkup choice = ReplyKeyboardMarkup.builder()
                .keyboardRow(new KeyboardRow(Collections.singletonList(rateHookahButton)))
                .build();
        return SendMessage.builder().chatId(chatId).text("Введи место, где вы попробовали сиё чудо").replyMarkup(choice).build();
    }

    public SendMessage sendRateKeyboard(Long chatId) {
        List<KeyboardButton> keyboardButtons = Stream.iterate(0, current -> current <= 10, current -> current + 1)
                .map(rating -> KeyboardButton.builder()
                        .text(rating.toString())
                        .build())
                .toList();
        ReplyKeyboardMarkup choice = ReplyKeyboardMarkup.builder()
                .keyboardRow(new KeyboardRow(keyboardButtons.subList(0, 4)))
                .keyboardRow(new KeyboardRow(keyboardButtons.subList(4, 7)))
                .keyboardRow(new KeyboardRow(keyboardButtons.subList(7, 11)))
                .build();
        return SendMessage.builder().chatId(chatId).text("Оцени кальян от 1 до 10").replyMarkup(choice).build();
    }

    public SendMessage sendInitialKeyboard(Long chatId, String message) {
        KeyboardButton startHookahButton = KeyboardButton.builder().text("Начать").build();
        KeyboardButton showAllRatesHookahButton = KeyboardButton.builder().text("Покажи мои оценки").build();
        ReplyKeyboardMarkup choice = ReplyKeyboardMarkup.builder()
                .keyboardRow(new KeyboardRow(List.of(startHookahButton, showAllRatesHookahButton)))
                .build();
        return SendMessage.builder().chatId(chatId).text(message).replyMarkup(choice).build();
    }

    private HookahUser meetAndGetUser(Update update, Message message, Long chatId) throws TelegramApiException {
        Optional<HookahUser> maybeHookahUser = hookahUserRepository.findHookahUserByChatId(chatId);
        boolean isUserExists = maybeHookahUser.isPresent();
        if (!isUserExists) {
            logger.atInfo().log(message.toString());
            HookahUser hookahUser = hookahUserRepository.save(HookahUser.builder()
                    .chatId(chatId)
                    .name(update.getMessage().getFrom().getFirstName())
                    .registeredDate(LocalDateTime.now())
                    .state(HookahUserState.START)
                    .build()
            );
            greetingMessage(hookahUser);
            execute(sendInitialKeyboard(chatId, "Оценим дуделку?"));
            return hookahUser;
        } else {
            return maybeHookahUser.get();
        }
    }

    private void greetingMessage(HookahUser hookahUser) throws TelegramApiException {
        execute(SendMessage.builder()
                .chatId(hookahUser.getChatId())
                .text("Приветствую, " +
                        hookahUser.getName() +
                        "! В этом приложении ты сможешь удобно сохранять вкусы и оценку кальянов, которые ты попробовал. Удачи!")
                .build()
        );
    }

    @Override
    public String getBotUsername() {
        return "owep_bot";
    }

    @Override
    public void onRegister() {
        logger.atInfo().log("HookahTestBot has been registered!");
        super.onRegister();
    }
}
