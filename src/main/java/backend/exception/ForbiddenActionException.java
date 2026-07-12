package backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an authenticated user is recognized, but isn't allowed to
 * perform the specific action they're attempting (e.g. editing someone
 * else's advertisement, or a non-admin trying to approve an ad). Distinct
 * from {@link InvalidCredentialsException} (401 — we don't know who you
 * are) — this is 403: we know who you are, and the answer is no.
 */
public class ForbiddenActionException extends ApiException {

    public ForbiddenActionException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
