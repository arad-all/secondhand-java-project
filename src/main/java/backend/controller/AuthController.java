package backend.controller;

import backend.controller.dto.LoginRequest;
import backend.controller.dto.LoginResponse;
import backend.controller.dto.RegisterRequest;
import backend.controller.dto.RegisterResponse;
import backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for registration and login. Bean validation ({@code @Valid})
 * checks the request's shape (blank fields, email format, etc.) before
 * either method body runs; everything past that — uniqueness checks,
 * password verification, account-status checks, JWT issuance — is business
 * logic delegated to {@link AuthService}. Any failure there (duplicate
 * username, bad credentials, blocked account) is thrown as one of the
 * {@code backend.exception} types and turned into the standard error JSON
 * shape by {@link backend.exception.GlobalExceptionHandler}, so this class
 * never needs its own try/catch.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
