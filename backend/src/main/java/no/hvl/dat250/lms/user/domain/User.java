package no.hvl.dat250.lms.user.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_id_generator")
    @SequenceGenerator(name = "user_id_generator", sequenceName = "user_id_seq", allocationSize = 1)
    @Column(name = "user_id", updatable = false)
    private Long userId;

    @NotBlank
    @Size(max = 255)
    @Column(name = "first_name", nullable = false, length = 255)
    private String firstName;

    @NotBlank
    @Size(max = 255)
    @Column(name = "last_name", nullable = false, length = 255)
    private String lastName;

    @Column(unique = true, nullable = false, updatable = false)
    private String email;

    @NotBlank
    @Size(max = 255)
    @Column(name = "password", nullable = false)
    private String passwordHash;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Access access = Access.STANDARD;

    /** Creates an empty user for JPA. */
    protected User() {}

    /**
     * Creates a user with STANDARD access. The caller must hash the password first.
     *
     * @param passwordHash the already encoded password
     * @param firstName the required first name
     * @param lastName the required last name
     */
    public User(String passwordHash, String firstName, String lastName) {
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    /** Sequence-generated IDs are available here, before the first INSERT. */
    @PrePersist
    private void generateEmail() {
        this.email = this.userId + "@epost.com";
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

    /** New accounts must replace their temporary password before normal use. */
    public boolean isMustChangePassword() { return this.mustChangePassword; }

    /**
     * Replaces the hash and clears the first-login requirement together.
     * A trusted auth service must authorize the change, verify the old password,
     * reject reuse, and encode the new password before calling this method.
     */
    public void changePassword(String newPasswordHash) {
        if (newPasswordHash == null || newPasswordHash.isBlank() || newPasswordHash.length() > 255) {
            throw new IllegalArgumentException("Invalid encoded password");
        }
        this.passwordHash = newPasswordHash;
        this.mustChangePassword = false;
    }

    /** Returns the email login identifier required by Spring Security's UserDetails contract. */
    @Override
    public String getUsername() { return this.email; }

    /** Returns the database-generated ID, or null before persistence. */
    public Long getUserId() { return this.userId; }

    /** Returns the generated email address, or null before persistence. */
    public String getEmail() { return this.email; }

    /** Returns the first name. */
    public String getFirstName() { return this.firstName; }

    /** Returns the last name. */
    public String getLastName() { return this.lastName; }

    /** Updates editable profile fields; the service must validate them first. */
    public void updateProfile(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    /** Returns the user's global access level. */
    public Access getAccess() { return this.access; }

    /** Sets global access; the calling service must authorize this change. */
    public void setAccess(Access access) { this.access = access; }
}
