package com.dmtryii.service.impl;

import com.dmtryii.entity.AppDocument;
import com.dmtryii.entity.AppPhoto;
import com.dmtryii.entity.AppUser;
import com.dmtryii.entity.RawData;
import com.dmtryii.exception.UploadFileException;
import com.dmtryii.repository.AppUserRepository;
import com.dmtryii.repository.RawDataRepository;
import com.dmtryii.service.AppUserService;
import com.dmtryii.service.FileService;
import com.dmtryii.service.MainService;
import com.dmtryii.service.ProduceService;
import com.dmtryii.service.enums.LinkType;
import com.dmtryii.service.enums.ServiceCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import static com.dmtryii.entity.enums.UserStates.BASIC_STATE;
import static com.dmtryii.entity.enums.UserStates.WAIT_FOR_EMAIL_STATE;
import static com.dmtryii.service.enums.ServiceCommand.*;

@Log4j
@RequiredArgsConstructor
@Service
public class MainServiceImpl implements MainService {

    private final RawDataRepository rawDataRepository;
    private final ProduceService produceService;
    private final AppUserRepository appUserRepository;
    private final FileService fileService;
    private final AppUserService appUserService;

    @Override
    public void processTextMessage(Update update) {
        saveRowData(update);

        var appUser = findOrSaveAppsUser(update);
        var userState = appUser.getState();
        var text = update.getMessage().getText();
        var output = "";

        var serviceCommand = ServiceCommand.fromValue(text);

        if(CANCEL.equals(serviceCommand)) {
            output = cancelProcess(appUser);
        } else if (BASIC_STATE.equals(userState)) {
            output = processServiceCommand(appUser, text);
        } else if (WAIT_FOR_EMAIL_STATE.equals(userState)) {
            output = appUserService.setEmail(appUser, text);
        } else {
            log.error("Unknown user state: " + userState);
            output = "Unknown error: Type /cancel and try again";
        }

        var chatId = update.getMessage().getChatId();
        sendAnswer(output, chatId);
    }

    @Override
    public void processDocMessage(Update update) {
        saveRowData(update);
        var appUser = findOrSaveAppsUser(update);
        var chatId = update.getMessage().getChatId();

        if(isNotAllowSendContend(chatId, appUser)) {
            return;
        }

        try {
            AppDocument doc = fileService.processDoc(update.getMessage());
            String link = fileService.generateLink(doc.getId(), LinkType.GET_DOC);
            var answer = "The document has been uploaded! Download link: " + link;
            sendAnswer(answer, chatId);
        } catch (UploadFileException e) {
            log.error(e);
            String error = "Unfortunately, the download failed. Try again later.";
            sendAnswer(error, chatId);
        }
    }

    @Override
    public void processPhotoMessage(Update update) {
        saveRowData(update);
        var appUser = findOrSaveAppsUser(update);
        var chatId = update.getMessage().getChatId();

        if(isNotAllowSendContend(chatId, appUser)) {
            return;
        }

        try {
            AppPhoto photo = fileService.processPhoto(update.getMessage());
            String link = fileService.generateLink(photo.getId(), LinkType.GET_PHOTO);
            var answer = "The photo has been uploaded! Download link: " + link;
            sendAnswer(answer, chatId);
        } catch (UploadFileException e) {
            log.error(e);
            String error = "Unfortunately, the download failed. Try again later.";
            sendAnswer(error, chatId);
        }
    }

    private boolean isNotAllowSendContend(Long chatId, AppUser appUser) {

        var userState = appUser.getState();

        if(!appUser.getIsActive()) {
            var error = "Register for further work!";
            sendAnswer(error, chatId);
            return true;
        } else if (!BASIC_STATE.equals(userState)) {
            var error = "Complete the previous command!";
            sendAnswer(error, chatId);
            return true;
        }
        return false;
    }

    private void sendAnswer(String output, Long chatId) {
        var sendMsg = SendMessage.builder()
                .text(output)
                .chatId(chatId)
                .build();
        produceService.produceAnswer(sendMsg);
    }

    private String cancelProcess(AppUser appUser) {
        appUser.setState(BASIC_STATE);
        appUserRepository.save(appUser);
        return "The command is cancelled";
    }

    private String processServiceCommand(AppUser appUser, String cmd) {

        var serviceCommand = ServiceCommand.fromValue(cmd);

        if(REGISTRATION.equals(serviceCommand)) {
            return appUserService.register(appUser);
        } else if (HELP.equals(serviceCommand)) {
            return help();
        } else if (START.equals(serviceCommand)) {
            return "Welcome, to see a list of available commands, type /help";
        } else {
            return "This command is not available! Type /help";
        }
    }

    private String help() {
        return "List of available commands: \n"
                + "/cancel - cancellation of command execution \n"
                + "/registration - user registration";
    }

    private AppUser findOrSaveAppsUser(Update update) {

        User telegramUser = update.getMessage().getFrom();

        var persistentAppUser = appUserRepository
                .findByTelegramUserId(telegramUser.getId());

        if (persistentAppUser.isEmpty()) {
            AppUser transientAppUser = AppUser.builder()
                    .telegramUserId(telegramUser.getId())
                    .username(telegramUser.getUserName())
                    .firstName(telegramUser.getFirstName())
                    .lastName(telegramUser.getLastName())
                    .isActive(false)
                    .state(BASIC_STATE)
                    .build();
            return appUserRepository.save(transientAppUser);
        }
        return persistentAppUser.get();
    }

    private void saveRowData(Update update) {
        RawData rawData = RawData.builder()
                .event(update)
                .build();
        rawDataRepository.save(rawData);
    }
}
