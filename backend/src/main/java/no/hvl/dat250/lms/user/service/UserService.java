package no.hvl.dat250.lms.user.service;

import no.hvl.dat250.lms.user.repository.UserRepository;
import no.hvl.dat250.lms.user.domain.User;
import java.util.Optional;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Provides user lookups for profile requests and Spring Security authentication. */
@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    /** Creates the service with its user repository. */
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Returns the user with the given ID, or an empty Optional if not found. */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
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
