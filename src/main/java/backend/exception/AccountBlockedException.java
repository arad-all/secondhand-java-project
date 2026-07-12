package backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a user whose account status is BLOCKED attempts to log in
 * or perform an action that requires an active account (e.g. sending a
 * chat message). Kept distinct from {@link InvalidCredentialsException}
 * because here the credentials ARE correct — the account itself is
 * disabled, which is worth telling the user plainly rather than hiding
 * behind a generic "invalid credentials" message. Maps to HTTP 403.
 */
public class AccountBlockedException extends ApiException {

    public AccountBlockedException() {
        super(HttpStatus.FORBIDDEN, "This account has been blocked.");
    }
}
