
package no.hvl.dat250.lms.content.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import no.hvl.dat250.lms.module.domain.Module;

@Entity
@Table(name = "assignments")
public class Assignment extends Content {

    @Column(nullable = false, length = 10000)
    private String instructions;

    @Column(name = "deadline")
    private Instant deadline;

    @Column(name = "allow_late_submission", nullable = false)
    private boolean allowLateSubmission;

    protected Assignment() {
        // Required by JPA
    }

    public Assignment(
            Module module,
            String title,
            int position,
            String instructions,
            Instant deadline,
            boolean allowLateSubmission) {

        super(module, title, position);

        if (instructions == null || instructions.isBlank()) {
            throw new IllegalArgumentException(
                "Assignment instructions must not be blank"
            );
        }

        this.instructions = instructions;
        this.deadline = deadline;
        this.allowLateSubmission = allowLateSubmission;
    }

    public String getInstructions() {
        return instructions;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public boolean isAllowLateSubmission() {
        return allowLateSubmission;
    }
}
