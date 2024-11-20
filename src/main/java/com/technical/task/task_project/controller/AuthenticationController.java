package com.technical.task.task_project.controller;

import com.technical.task.task_project.service.AuthenticationService;
import com.technical.task.task_project.service.dto.AuthenticationRequestDTO;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    @PostMapping(value = "/authenticate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String createToken(@Valid @RequestBody AuthenticationRequestDTO request) throws Exception {
        return authenticationService.authenticate(request);
    }
}
