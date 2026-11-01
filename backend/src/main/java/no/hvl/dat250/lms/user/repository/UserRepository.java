package no.hvl.dat250.lms.user.repository;

import no.hvl.dat250.lms.user.domain.User;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

/**
 * Provides user lookups and inherits persistence operations such as save and delete.
 */
public interface UserRepository extends CrudRepository<User, Long> {

    /** Returns the matching user, or an empty Optional if the user ID is unknown. */
    public Optional<User> findByUserId(Long userId);

    /** Returns the matching user, or an empty Optional if the email is unknown. */
    public Optional<User> findByEmail(String email);

}
