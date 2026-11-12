package no.hvl.dat250.lms.user.domain;

import jakarta.persistence.*;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    private Access access;

    public User() {}

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.access = Access.STANDARD;
    }

    public String getEmail() { return this.email; }

    public Long getId() { return this.id; }

    public Access getAccess() { return this.access; }

    public void setAccess(Access access) { this.access = access; }
}