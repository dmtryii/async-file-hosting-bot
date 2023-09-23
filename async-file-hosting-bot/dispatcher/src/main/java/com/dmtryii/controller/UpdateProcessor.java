package com.dmtryii.controller;

import com.dmtryii.config.RabbitConfig;
import com.dmtryii.service.UpdateProducer;
import com.dmtryii.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Log4j
@RequiredArgsConstructor
@Component
public class UpdateProcessor {

    private TelegramBot telegramBot;
    private final MessageUtil messageUtil;
    private final UpdateProducer updateProducer;
    private final RabbitConfig rabbitConfig;

    public void registerBot(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    public void processUpdate(Update update) {
        if(update == null) {
            log.debug("Received update is null");
            return;
        }

        if(update.hasMessage()) {
            distributeMessageByType(update);
        } else {
            log.error("Unsupported message type is received: " + update);
        }
    }

    private void distributeMessageByType(Update update) {
        var message = update.getMessage();

        if(message.hasText()) {
            processTextMessage(update);
        } else if (message.hasDocument()) {
            processTextDocument(update);
        } else if (message.hasPhoto()) {
            processTextPhoto(update);
        } else {
            setUnsupportedMessageTypeView(update);
        }
    }

    private void setUnsupportedMessageTypeView(Update update) {
        var sendMsg = messageUtil.generateSendMessageWithText(update,
                "Unsupported Message Type");
        setView(sendMsg);
    }

    private void setFileIsReceivedView(Update update) {
        var sendMsg = messageUtil.generateSendMessageWithText(update,
                "File received. Processing is in progress...");
        setView(sendMsg);
    }

    public void setView(SendMessage sendMsg) {
        telegramBot.sendAnswerMessage(sendMsg);
    }

    private void processTextPhoto(Update update) {
        updateProducer.produce(rabbitConfig.getPhotoMessageUpdateQueue(), update);
        setFileIsReceivedView(update);
    }

    private void processTextDocument(Update update) {
        updateProducer.produce(rabbitConfig.getDocMessageUpdateQueue(), update);
        setFileIsReceivedView(update);
    }

    private void processTextMessage(Update update) {
        updateProducer.produce(rabbitConfig.getTextMessageUpdateQueue(), update);
    }
}
