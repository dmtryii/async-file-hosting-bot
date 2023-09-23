package com.dmtryii.util;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class MessageUtil {
    public SendMessage generateSendMessageWithText(Update update, String text) {
        var message = update.getMessage();
        return SendMessage.builder()
                .chatId(message.getChatId())
                .text(text)
                .build();
    }
}
