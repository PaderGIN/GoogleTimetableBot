package my.googletimetablebot.services;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import my.googletimetablebot.config.BotConfig;
import my.googletimetablebot.models.User;
import my.googletimetablebot.models.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private UserRepository userRepository;

    final BotConfig config;

    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> commands = new ArrayList<>();
        commands.add(new BotCommand("/info", "get some information about functions"));
        commands.add(new BotCommand("/start", "get start"));
        commands.add(new BotCommand("/add_google_account", "set google data configuration"));

        try {
            this.execute(new SetMyCommands(commands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            ;
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();


            switch (messageText) {
                case "/start":
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    checkReg(update.getMessage());
                    break;
                case "/info":
                    sendMessage(chatId, "This bot can control your events in Google Calendar");
                    sendMessage(chatId, "If you are haven't already added your google calendar data," +
                            " you can do it with command '/add google account'");
                    break;
                case "/add_google_account":
                    addGoogleAccount(update.getMessage());
                    break;
                default:
                    sendMessage(chatId, "Sorry, command was not recognized");
                    break;
            }
        }
    }

    private void addGoogleAccount(Message msg) {
        var chatId = msg.getChatId();
        var chat = msg.getChat();

        sendMessage(chatId, "Enter your data:");
    }

    private void checkReg(Message msg) {

        if (userRepository.findById(msg.getChatId()).isEmpty()){
            var chatId = msg.getChatId();
            var chat = msg.getChat();

            User user = new User();

            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());

            userRepository.save(user);

        }
    }

    private void startCommandReceived(long chatId, String name) {

        String answer = "Hi, " + name + ", nice to meet you!";
        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        ReplyKeyboardMarkup plate = new ReplyKeyboardMarkup();

        List<KeyboardRow> kRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();

        row.add("/info");
        row.add("/start");

//        KeyboardRow row2 = new KeyboardRow();
//
//        row2.add("something");
//        row2.add("another one");
//
        kRows.add(row);
//        kRows.add(row2);

        plate.setKeyboard(kRows);
        message.setReplyMarkup(plate);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            ;
        }
    }
}