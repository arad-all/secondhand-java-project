package backend.controller.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for sending a chat message — either the first message to
 * an advertisement's seller ({@code POST /api/advertisements/{id}/messages})
 * or a reply within an existing thread
 * ({@code POST /api/conversations/{id}/messages}). No sender field: the
 * sender is always the authenticated caller, never taken from the body.
 */
public record SendMessageRequest(
        @NotBlank(message = "Message content must not be blank.")
        String content
) {
}
