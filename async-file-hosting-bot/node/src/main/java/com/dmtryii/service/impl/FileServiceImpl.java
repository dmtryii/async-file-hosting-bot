package com.dmtryii.service.impl;

import com.dmtryii.entity.AppDocument;
import com.dmtryii.entity.AppPhoto;
import com.dmtryii.entity.BinaryContent;
import com.dmtryii.exception.UploadFileException;
import com.dmtryii.repository.AppDocumentRepository;
import com.dmtryii.repository.AppPhotoRepository;
import com.dmtryii.repository.BinaryContentRepository;
import com.dmtryii.service.FileService;
import com.dmtryii.service.enums.LinkType;
import com.dmtryii.utils.CryptoTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

@Log4j
@RequiredArgsConstructor
@Service
public class FileServiceImpl implements FileService {

    @Value("${token}")
    private String token;
    @Value("${service.file_info.url}")
    private String fileInfoUri;
    @Value("${service.file_storage.url}")
    private String fileStorageUri;
    @Value("${link.address}")
    private String linkAddress;
    private final AppDocumentRepository appDocumentRepository;
    private final AppPhotoRepository appPhotoRepository;
    private final BinaryContentRepository binaryContentRepository;
    private final CryptoTool cryptoTool;

    @Override
    public AppDocument processDoc(Message externMessage) {

        Document telegramDoc = externMessage.getDocument();
        String fileId = telegramDoc.getFileId();
        ResponseEntity<String> response = getFilePath(fileId);

        if(isGoodResponse(response)) {
            BinaryContent persistentBinaryContent =
                    getPersistentBinaryContent(response);
            AppDocument transientAppDoc =
                    buildTransientAppDoc(telegramDoc, persistentBinaryContent);
            return appDocumentRepository.save(transientAppDoc);
        } else {
            throw new UploadFileException("Bad request from telegram service " + response);
        }
    }

    @Override
    public AppPhoto processPhoto(Message externMessage) {

        var photoSizePhoto = externMessage.getPhoto().size();
        var photoIdx = photoSizePhoto > 1 ?
                externMessage.getPhoto().size() - 1 : 0;

        PhotoSize telegramPhoto = externMessage.getPhoto().get(photoIdx);
        String fileId = telegramPhoto.getFileId();
        ResponseEntity<String> response = getFilePath(fileId);

        if(isGoodResponse(response)) {
            BinaryContent persistentBinaryContent =
                    getPersistentBinaryContent(response);
            AppPhoto transientAppPhoto =
                    buildTransientAppPhoto(telegramPhoto, persistentBinaryContent);
            return appPhotoRepository.save(transientAppPhoto);
        } else {
            throw new UploadFileException("Bad request from telegram service " + response);
        }
    }

    private BinaryContent getPersistentBinaryContent(ResponseEntity<String> response) {
        String filePath = getFilePath(response);
        byte[] fileInByte = downloadFile(filePath);
        BinaryContent transientBinaryContent = BinaryContent.builder()
                .fileAsArrayOfBytes(fileInByte)
                .build();
        return binaryContentRepository.save(transientBinaryContent);
    }

    private String getFilePath(ResponseEntity<String> response) {
        JSONObject jsonObject = new JSONObject(response.getBody());
        return jsonObject
                .getJSONObject("result")
                .getString("file_path");
    }

    private AppDocument buildTransientAppDoc(Document telegramDoc,
                                             BinaryContent persistentBinaryContent) {
        return AppDocument.builder()
                .telegramFileId(telegramDoc.getFileId())
                .docName(telegramDoc.getFileName())
                .binaryContent(persistentBinaryContent)
                .mimeType(telegramDoc.getMimeType())
                .size(telegramDoc.getFileSize())
                .build();
    }

    private AppPhoto buildTransientAppPhoto(PhotoSize telegramPhoto,
                                            BinaryContent persistentBinaryContent) {
        return AppPhoto.builder()
                .telegramFileId(telegramPhoto.getFileId())
                .binaryContent(persistentBinaryContent)
                .size(telegramPhoto.getFileSize())
                .build();
    }

    private ResponseEntity<String> getFilePath(String fileId) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> request = new HttpEntity<>(headers);

        return restTemplate.exchange(
                fileInfoUri,
                HttpMethod.GET,
                request,
                String.class,
                token, fileId
        );
    }

    private byte[] downloadFile(String filePath) {

        String fullUri = fileStorageUri.replace("{token}", token)
                .replace("{filePath}", filePath);
        URL urlObj = null;
        try {
            urlObj = new URL(fullUri);
        } catch (MalformedURLException e) {
            throw new UploadFileException(e);
        }

        // TODO optimisation
        try(InputStream is = urlObj.openStream()){
            return is.readAllBytes();
        } catch (IOException e) {
            throw new UploadFileException(urlObj.toExternalForm(), e);
        }
    }

    @Override
    public String generateLink(Long docId, LinkType linkType) {
        var hash = cryptoTool.hashOf(docId);
        return "http://" + linkAddress +
                "/" + linkType +
                "?id=" + hash;
    }

    private boolean isGoodResponse(ResponseEntity<String> response) {
        return response.getStatusCode().is2xxSuccessful()
                && Objects.nonNull(response.getBody());
    }
}
