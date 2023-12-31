package com.dmtryii.controller;

import com.dmtryii.service.UserActivationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/user")
@RestController
public class ActivationController {

    private final UserActivationService userActivationService;

    @GetMapping("/activation")
    public ResponseEntity<?> activation(@RequestParam String id) {
        var res = userActivationService.activation(id);
        if (res) {
            return ResponseEntity.ok().body("Registration is successful");
        }
        return ResponseEntity.internalServerError().build();
    }
}
