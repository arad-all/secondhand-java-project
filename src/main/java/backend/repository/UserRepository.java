package backend.repository;

import backend.model.entity.User;
import backend.model.enums.AccountStatus;
import backend.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Data access for {@link User}. Username and phone number are both unique
 * at the database level, so lookups by either are provided for
 * authentication / registration checks.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByPhoneNumber(String phoneNumber);

    boolean existsByUsername(String username);

    boolean existsByPhoneNumber(String phoneNumber);

    List<User> findByRole(Role role);

    List<User> findByStatus(AccountStatus status);
}
