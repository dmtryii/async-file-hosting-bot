package com.dmtryii.service.impl;

import com.dmtryii.repository.AppUserRepository;
import com.dmtryii.service.UserActivationService;
import com.dmtryii.utils.CryptoTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Service;

@Log4j
@RequiredArgsConstructor
@Service
public class UserActivationServiceImpl implements UserActivationService {

    private final AppUserRepository appUserRepository;
    private final CryptoTool cryptoTool;

    @Override
    public boolean activation(String encryptUserId) {
        var userId = cryptoTool.idOf(encryptUserId);
        var optional = appUserRepository.findById(userId);

        if(optional.isPresent()) {
            var user = optional.get();
            user.setIsActive(true);
            appUserRepository.save(user);
            return true;
        }
        return false;
    }
}
