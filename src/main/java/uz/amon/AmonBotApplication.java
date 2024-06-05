package uz.amon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import uz.amon.bot.AmonBot;

@SpringBootApplication
public class AmonBotApplication
{
    @Bean
    public TelegramBotsApi telegramBotsApi(AmonBot amonBot) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi=new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(amonBot);
        return telegramBotsApi;
    }
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public static void main(String[] args)
    {
        SpringApplication.run(AmonBotApplication.class, args);
    }

}
