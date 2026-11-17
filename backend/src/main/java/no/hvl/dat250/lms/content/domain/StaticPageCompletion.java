package no.hvl.dat250.lms.content.domain;

import jakarta.persistence.*;
import java.time.Instant;
import no.hvl.dat250.lms.enrollment.domain.Enrollment;

@Entity
@Table(name = "static_page_completions", uniqueConstraints = @UniqueConstraint(columnNames = {"content_id", "enrollment_id"}))
public class StaticPageCompletion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id", nullable = false)
    private StaticPage content;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;
    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;
    protected StaticPageCompletion() {}
    public StaticPageCompletion(StaticPage content, Enrollment enrollment, Instant completedAt) {
        this.content = java.util.Objects.requireNonNull(content);
        this.enrollment = java.util.Objects.requireNonNull(enrollment);
        this.completedAt = java.util.Objects.requireNonNull(completedAt);
    }
}
