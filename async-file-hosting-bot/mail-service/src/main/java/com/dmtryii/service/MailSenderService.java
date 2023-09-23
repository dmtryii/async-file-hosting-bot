package com.dmtryii.service;

import com.dmtryii.dto.MailParams;

public interface MailSenderService {
    void send(MailParams mailParams);
}
