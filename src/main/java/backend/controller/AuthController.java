package backend.controller;

import backend.controller.dto.LoginRequest;
import backend.controller.dto.LoginResponse;
import backend.controller.dto.RegisterRequest;
import backend.controller.dto.RegisterResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for registration and login.
 *
 * IMPORTANT: This controller intentionally contains no business logic,
 * no password hashing, no JWT generation and no database access. Those
 * belong in a future AuthService / UserService, once the team member
 * responsible for backend services implements them. Every method below
 * returns a placeholder response so the frontend can be built and tested
 * against a stable contract in the meantime.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/register")
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        // TODO: delegate to a UserService (not implemented yet) that should:
        //   1. Check that username and phoneNumber are not already taken.
        //   2. Hash the password (e.g. BCrypt) before persisting it.
        //   3. Save a new User with role = USER and status = ACTIVE.
        //   4. Return a proper error (e.g. 409 Conflict) on duplicate username.
        return new RegisterResponse(
                "Registration request received for '" + request.username()
                        + "'. Backend registration logic is not implemented yet.");
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        // TODO: delegate to an AuthService (not implemented yet) that should:
        //   1. Look up the User by username.
        //   2. Compare the given password against the stored password hash.
        //   3. Reject login if the account status is BLOCKED.
        //   4. Generate a real, signed JWT containing user id, username and role.
        //   5. Return 401 for invalid credentials instead of a fake success.
        return new LoginResponse(
                "placeholder-jwt-token",
                0L,
                request.username(),
                "USER");
    }
}
