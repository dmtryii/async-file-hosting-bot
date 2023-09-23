package com.dmtryii.service.impl;

import com.dmtryii.entity.AppDocument;
import com.dmtryii.entity.AppPhoto;
import com.dmtryii.repository.AppDocumentRepository;
import com.dmtryii.repository.AppPhotoRepository;
import com.dmtryii.service.FileService;
import com.dmtryii.utils.CryptoTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Service;

@Log4j
@RequiredArgsConstructor
@Service
public class FileServiceImpl implements FileService {

    private final AppDocumentRepository appDocumentRepository;
    private final AppPhotoRepository appPhotoRepository;
    private final CryptoTool cryptoTool;

    @Override
    public AppDocument getDocument(String hash) {
        var docId = cryptoTool.idOf(hash);
        if (docId == null) return null;
        return appDocumentRepository.findById(docId).orElse(null);
    }

    @Override
    public AppPhoto getPhoto(String hash) {
        var photoId = cryptoTool.idOf(hash);
        if (photoId == null) return null;
        return appPhotoRepository.findById(photoId).orElse(null);
    }
}
