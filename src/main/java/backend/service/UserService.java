package backend.service;

import backend.exception.ForbiddenActionException;
import backend.exception.InvalidStateTransitionException;
import backend.exception.ResourceNotFoundException;
import backend.model.entity.User;
import backend.model.enums.AccountStatus;
import backend.model.enums.Role;
import backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Admin user-moderation logic (plan §"Moderation"). Grouped with
 * {@link AdvertisementService}'s {@code approve}/{@code reject}/
 * {@code listPendingReview} under the same phase because both need
 * role-based (ADMIN) authorization working correctly — that restriction
 * is enforced at the controller/security layer ({@code /api/admin/**} in
 * {@code SecurityConfig}), not here, exactly like every other admin-only
 * rule in this codebase.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /** Every user, optionally narrowed to one account status (e.g. only BLOCKED accounts). */
    @Transactional(readOnly = true)
    public List<User> listUsers(AccountStatus statusFilter) {
        return (statusFilter != null) ? userRepository.findByStatus(statusFilter) : userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found."));
    }

    /** Same lookup as {@link #getById}, by username instead — backs the public seller-profile endpoint. */
    @Transactional(readOnly = true)
    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User '" + username + "' not found."));
    }

    /**
     * Blocks a user's account, preventing further logins (see
     * {@code AuthService.login}). An admin can't block themselves or
     * another admin through this action — this action is for moderating
     * regular users, and one admin's access shouldn't hinge on another
     * admin's judgment call. Blocking an already-blocked account is
     * rejected as an invalid transition rather than silently doing
     * nothing.
     */
    @Transactional
    public User blockUser(Long targetUserId, Long currentAdminId) {
        User target = getById(targetUserId);

        if (target.getId().equals(currentAdminId) || target.getRole() == Role.ADMIN) {
            throw new ForbiddenActionException("Admin accounts cannot be blocked through this action.");
        }
        if (target.getStatus() == AccountStatus.BLOCKED) {
            throw new InvalidStateTransitionException("User is already blocked.");
        }

        target.setStatus(AccountStatus.BLOCKED);
        return target;
    }

    /** Restores a blocked account to active. Unblocking an already-active account is rejected. */
    @Transactional
    public User unblockUser(Long targetUserId) {
        User target = getById(targetUserId);

        if (target.getStatus() == AccountStatus.ACTIVE) {
            throw new InvalidStateTransitionException("User is already active.");
        }

        target.setStatus(AccountStatus.ACTIVE);
        return target;
    }
}
