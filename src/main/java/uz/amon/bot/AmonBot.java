package uz.amon.bot;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
@Component
@RequiredArgsConstructor
public class AmonBot extends TelegramLongPollingBot
{
    @Value("${telegram.bot.token}")
    public String token;

    @Value("${telegram.bot.username}")
    public String username;

    @Value("${photo.uploadDir}")
    public String imgLocation;


    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update)
    {
        if (update.hasMessage())
        {
            String text = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();
            System.out.println("chatId = " + chatId);

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(text);
            sendMessage.setReplyMarkup(update.getMessage().getReplyMarkup());

            switch (text)
            {
                case "/start": startHandler(update);

                default: execute(sendMessage);
            }

        }
    }

    private void startHandler(Update update)
    {

    }

}
