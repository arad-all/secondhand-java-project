package backend.service;

import backend.controller.dto.LoginRequest;
import backend.controller.dto.LoginResponse;
import backend.controller.dto.RegisterRequest;
import backend.controller.dto.RegisterResponse;
import backend.exception.AccountBlockedException;
import backend.exception.DuplicateResourceException;
import backend.exception.InvalidCredentialsException;
import backend.model.entity.User;
import backend.model.enums.AccountStatus;
import backend.model.enums.Role;
import backend.repository.UserRepository;
import backend.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Real registration and login logic, replacing the placeholder bodies
 * that used to live directly in {@code AuthController}. This is the first
 * service in the project, so it's also the first place the
 * controller → service → repository flow described in the plan actually
 * happens end to end.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Constructor injection (no {@code @Autowired} needed on a single
     * constructor since Spring 4.3+) rather than Lombok's
     * {@code @RequiredArgsConstructor}, so this class's dependencies are
     * explicit and don't rely on annotation processing to see. Either
     * style is fine given the rest of the codebase already uses Lombok
     * for entities — this file just spells the constructor out.
     */
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Registers a new user.
     * <ul>
     *   <li>Username, phone number, and email must each be unique — checked
     *       here (not just relying on the DB's unique constraints) so a
     *       collision comes back as a clear 409 {@link DuplicateResourceException}
     *       instead of a raw constraint-violation exception bubbling up.</li>
     *   <li>The password is hashed before it's ever assigned to the entity —
     *       {@link User#getPassword()} must never hold a raw password, even
     *       transiently.</li>
     *   <li>Role and account status are never taken from the request — every
     *       new registration is a plain {@link Role#USER} with
     *       {@link AccountStatus#ACTIVE}, matching the entity's own defaults.
     *       There's deliberately no way for a client to register as ADMIN.</li>
     * </ul>
     * The whole check-then-insert sequence is wrapped in one transaction so
     * a race between the uniqueness checks and the save can't leave the
     * database in a half-written state.
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username '" + request.username() + "' is already taken.");
        }
        if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new DuplicateResourceException("Phone number '" + request.phoneNumber() + "' is already registered.");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email '" + request.email() + "' is already registered.");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setPhoneNumber(request.phoneNumber());
        user.setEmail(request.email());
        // role and status are left at the entity's own defaults (USER / ACTIVE) —
        // never set from the request.

        userRepository.save(user);

        return new RegisterResponse("Registration successful. You can now log in.");
    }

    /**
     * Authenticates a user and issues a JWT.
     * <ul>
     *   <li>"Username not found" and "password doesn't match" both come back
     *       as the same {@link InvalidCredentialsException} with the same
     *       message, so a caller can't use this endpoint to enumerate which
     *       usernames exist.</li>
     *   <li>A correct password on a {@link AccountStatus#BLOCKED} account is
     *       rejected with a distinct {@link AccountBlockedException} — unlike
     *       the case above, telling a legitimately-blocked user *why* they
     *       can't log in isn't a security leak, and is more useful to them.</li>
     * </ul>
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        if (user.getStatus() == AccountStatus.BLOCKED) {
            throw new AccountBlockedException();
        }

        String token = jwtService.generateToken(user);

        return new LoginResponse(token, user.getId(), user.getUsername(), user.getRole().name());
    }
}
