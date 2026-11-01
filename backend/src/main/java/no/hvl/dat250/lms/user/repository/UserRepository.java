package no.hvl.dat250.lms.user.repository;

import no.hvl.dat250.lms.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Provides user lookups and inherits persistence operations such as save and delete.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** Changes credentials only if the hash verified by auth is still current. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update User u set u.passwordHash = :newHash, u.mustChangePassword = false
            where u.userId = :userId and u.passwordHash = :expectedHash
            """)
    int replacePasswordIfCurrent(@Param("userId") Long userId,
                                 @Param("expectedHash") String expectedHash,
                                 @Param("newHash") String newHash);

    /** Name/email substring search, ignoring case, or an exact numeric user ID. */
    Page<User> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrUserId(
            String firstName, String lastName, String email, Long userId, Pageable pageable);

    /** Returns the matching user, or an empty Optional if the user ID is unknown. */
    public Optional<User> findByUserId(Long userId);

    /** Returns the matching user, or an empty Optional if the email is unknown. */
    public Optional<User> findByEmail(String email);

}
