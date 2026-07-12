package backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an operation would move an entity's status field along a
 * path its lifecycle doesn't allow — e.g. approving an advertisement
 * that isn't PENDING_REVIEW, or marking an already-SOLD ad as sold again.
 * Maps to HTTP 409, since the request is well-formed but conflicts with
 * the resource's current state.
 */
public class InvalidStateTransitionException extends ApiException {

    public InvalidStateTransitionException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
