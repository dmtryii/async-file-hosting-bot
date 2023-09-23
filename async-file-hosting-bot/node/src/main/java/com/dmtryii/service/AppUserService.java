package com.dmtryii.service;

import com.dmtryii.entity.AppUser;

public interface AppUserService {
    String register(AppUser appUser);
    String setEmail(AppUser appUser, String email);
}
