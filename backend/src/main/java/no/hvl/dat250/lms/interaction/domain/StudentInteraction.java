package no.hvl.dat250.lms.interaction.domain;

import no.hvl.dat250.lms.content.domain.Content;
import no.hvl.dat250.lms.user.domain.User;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "student_interactions",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"student_id", "content_id"}
        )
)
public class StudentInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    protected StudentInteraction() {};

    public StudentInteraction(User student, Content content) {
        this.student = student;
        this.content = content;
    }

    public Long getId() {
        return this.id;
    }

    public User getStudent() {
        return this.student;
    }

    public Content content() {
        return this.content;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public boolean isCompleted() {
        return completedAt != null;
    }

    public void markCompleted() {
        if (completedAt == null) {
            completedAt = LocalDateTime.now();
        }
    }
}
