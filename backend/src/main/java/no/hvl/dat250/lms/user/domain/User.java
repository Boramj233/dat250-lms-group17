package no.hvl.dat250.lms.user.domain;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * A persisted user account that also provides user details to Spring Security.
 */
@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Access access = Access.STANDARD;

    /** Creates an empty user for JPA. */
    public User() {}

    /**
     * Creates a user with STANDARD access. The caller must hash the password first.
     *
     * @param username the user's username
     * @param email the user's email address
     * @param passwordHash the already encoded password
     */
    public User(String username, String email, String passwordHash) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    /**
     * Returns no authorities; global Access is not yet mapped to Spring Security roles.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    /** Returns the password hash, as required by Spring Security. */
    @Override
    public String getPassword() { return this.passwordHash; }

    /** Returns the user's username. */
    @Override
    public String getUsername() { return this.username; }

    /** Returns the database-generated ID, or null before persistence. */
    public Long getId() { return this.id; }

    /** Returns the user's email address. */
    public String getEmail() { return this.email; }

    /** Returns the user's global access level. */
    public Access getAccess() { return this.access; }

    /** Sets global access; the calling service must authorize this change. */
    public void setAccess(Access access) { this.access = access; }
}
