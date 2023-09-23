package com.dmtryii.service.impl;

import com.dmtryii.dto.MailParams;
import com.dmtryii.entity.AppUser;
import com.dmtryii.repository.AppUserRepository;
import com.dmtryii.service.AppUserService;
import com.dmtryii.utils.CryptoTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import static com.dmtryii.entity.enums.UserStates.BASIC_STATE;
import static com.dmtryii.entity.enums.UserStates.WAIT_FOR_EMAIL_STATE;

@Log4j
@RequiredArgsConstructor
@Service
public class AppUserServiceImpl implements AppUserService {

    private final AppUserRepository appUserRepository;
    private final CryptoTool cryptoTool;
    private final RabbitTemplate rabbitTemplate;

    @Value("${spring.rabbitmq.queues.registration-mail}")
    private String registrationMailQueue;

    @Override
    public String register(AppUser appUser) {

        if (appUser.getIsActive()) {
            return "You are already registered";
        } else if (appUser.getEmail() != null) {
            return "You have already been sent an email" +
                    " to confirm your registration";
        }
        appUser.setState(WAIT_FOR_EMAIL_STATE);
        appUserRepository.save(appUser);
        return "Enter your email:";
    }

    @Override
    public String setEmail(AppUser appUser, String email) {

        try {
            InternetAddress emailAddress = new InternetAddress(email);
            emailAddress.validate();
        } catch (AddressException e) {
            return "Please enter a valid address";
        }

        var appUserOptional = appUserRepository.findByEmail(email);

        if(appUserOptional.isEmpty()) {

            appUser.setEmail(email);
            appUser.setState(BASIC_STATE);
            appUser = appUserRepository.save(appUser);

            var cryptoUserId = cryptoTool.hashOf(appUser.getId());

            sendRegistrationMail(cryptoUserId, email);

            return "Follow the link in the letter sent to e-mail address " + email;
        } else {
            return "This email address is already in use by another user";
        }
    }

    private void sendRegistrationMail(String cryptoUserId, String email) {
        var mailParams = MailParams.builder()
                .id(cryptoUserId)
                .emailTo(email)
                .build();
        rabbitTemplate.convertAndSend(registrationMailQueue, mailParams);
    }
}
