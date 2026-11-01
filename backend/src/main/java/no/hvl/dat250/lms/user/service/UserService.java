package no.hvl.dat250.lms.user.service;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import no.hvl.dat250.lms.user.api.dto.UpdateUserRequest;
import no.hvl.dat250.lms.user.repository.UserRepository;
import no.hvl.dat250.lms.user.domain.User;
import java.util.Optional;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Provides user lookups for profile requests and Spring Security authentication. */
@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final Validator validator;

    /** Creates the service with its user repository. */
    public UserService(UserRepository userRepository, Validator validator) {
        this.userRepository = userRepository;
        this.validator = validator;
    }

    /** Returns the user with the given ID, or an empty Optional if not found. */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Replaces names without changing userId, email, password, or global access.
     * The caller must obtain the target ID from the authenticated identity.
     */
    @Transactional
    public User updateProfile(Long id, UpdateUserRequest request) {
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        User user = userRepository.findById(id).orElseThrow(UserNotFoundException::new);
        user.updateProfile(request.firstName(), request.lastName());
        return userRepository.save(user);
    }

    /**
     * Loads an account by email for Spring Security, despite the interface method name.
     * This lookup does not verify the password.
     *
     * @param email the email used to identify the account
     * @return the stored user details
     * @throws UsernameNotFoundException if the email is unknown
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found"));
    }

}
