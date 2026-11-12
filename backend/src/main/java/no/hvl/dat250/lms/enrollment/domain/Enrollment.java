
package no.hvl.dat250.lms.enrollment.domain;

import jakarta.persistence.*;

import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.course.domain.Course;

@Entity
@Table(
    name = "enrollments",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"user_id", "course_id"}
    )
)
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(name = "course_role", nullable = false)
    private CourseRole role;

    @Column(nullable = false)
    private boolean active = true;

    protected Enrollment() {
        // Required by JPA
    }

    public Enrollment(
            User user,
            Course course,
            CourseRole role) {

        if (user == null || course == null || role == null) {
            throw new IllegalArgumentException(
                "User, Course and Role are required");
        }

        this.user = user;
        this.course = course;
        this.role = role;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Course getCourse() { return course; }
    public CourseRole getRole() { return role; }
    public boolean isActive() { return active; }

    public void deactivate() {
        this.active = false;
    }

    public void reactivate() {
        this.active = true;
    }
}
