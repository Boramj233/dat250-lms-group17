package no.hvl.dat250.lms.user.repository;

import no.hvl.dat250.lms.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Provides user lookups and inherits persistence operations such as save and delete.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** Name/email substring search, ignoring case, or an exact numeric user ID. */
    Page<User> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrUserId(
            String firstName, String lastName, String email, Long userId, Pageable pageable);

    /** Returns the matching user, or an empty Optional if the user ID is unknown. */
    public Optional<User> findByUserId(Long userId);

    /** Returns the matching user, or an empty Optional if the email is unknown. */
    public Optional<User> findByEmail(String email);

}
