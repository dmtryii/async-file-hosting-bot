package com.dmtryii.service;

import com.dmtryii.dto.MailParams;

public interface ConsumerService {
    void consumeRegistrationMail(MailParams mailParams);
}
