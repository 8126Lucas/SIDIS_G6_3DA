package com.sidis.authservice.Authentication;

import com.sidis.authservice.Authentication.application.AuthenticateCollaboratorUseCase;
import com.sidis.authservice.Authentication.application.LoginRequest;
import com.sidis.authservice.Authentication.application.TokenResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticateCollaboratorUseCase authenticateCollaborator;

    public AuthController(AuthenticateCollaboratorUseCase authenticateCollaborator) {
        this.authenticateCollaborator = authenticateCollaborator;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authenticateCollaborator.execute(request);
    }
}
