package com.dmtryii.service;

import com.dmtryii.entity.AppDocument;
import com.dmtryii.entity.AppPhoto;

public interface FileService {
    AppDocument getDocument(String hash);
    AppPhoto getPhoto(String hash);
}
