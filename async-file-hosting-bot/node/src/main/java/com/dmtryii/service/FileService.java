package com.dmtryii.service;

import com.dmtryii.entity.AppDocument;
import com.dmtryii.entity.AppPhoto;
import com.dmtryii.service.enums.LinkType;
import org.telegram.telegrambots.meta.api.objects.Message;

public interface FileService {
    AppDocument processDoc(Message externMessage);
    AppPhoto processPhoto(Message externMessage);
    String generateLink(Long docId, LinkType linkType);
}
