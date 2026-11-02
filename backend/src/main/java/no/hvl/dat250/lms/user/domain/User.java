package no.hvl.dat250.lms.user.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.security.Principal;

/**
 * Local application profile. Credentials and login belong to the identity provider.
 */
@Entity
@Table(name = "users")
public class User implements Principal {

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

    @NotBlank
    @Email
    @Size(max = 255)
    @Column(unique = true, nullable = false, updatable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Access access = Access.STANDARD;

    /** Creates an empty user for JPA. */
    protected User() {}

    /**
     * Creates a local profile with STANDARD access and the identity provider's email.
     *
     * @param email the email matching the external identity
     * @param firstName the required first name
     * @param lastName the required last name
     */
    public User(String email, String firstName, String lastName) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    /** Local principal name; auth must map the verified external identity to this profile. */
    @Override
    public String getName() { return this.email; }

    /** Returns the database-generated ID, or null before persistence. */
    public Long getUserId() { return this.userId; }

    /** Returns the email matching the external identity. */
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
