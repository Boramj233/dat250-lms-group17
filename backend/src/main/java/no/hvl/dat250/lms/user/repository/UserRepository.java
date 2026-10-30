package no.hvl.dat250.lms.user.repository;

import no.hvl.dat250.lms.user.domain.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

public interface UserRepository extends CrudRepository<User, Long>, UserDetailsService {

    public Optional<User> findByUsername(String username);

    public Optional<User> findByEmail(String email);

    @Override
    default UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {
        return findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "user not found: " + username
                        ));
    }
}