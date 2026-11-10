
package no.hvl.dat250.lms.content.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import no.hvl.dat250.lms.module.domain.Module;

@Entity
@Table(name = "coding_exercises")
public class CodingExercise extends Content {

    @Column(nullable = false, length = 10000)
    private String instructions;

    @Column(
        name = "starter_repository_url",
        nullable = false,
        length = 2048
    )
    private String starterRepositoryUrl;

    @Column(name = "deadline")
    private Instant deadline;

    @Column(name = "allow_late_submission", nullable = false)
    private boolean allowLateSubmission;

    protected CodingExercise() {
        // Required by JPA
    }

    public CodingExercise(
            Module module,
            String title,
            int position,
            String instructions,
            String starterRepositoryUrl,
            Instant deadline,
            boolean allowLateSubmission) {

        super(module, title, position);

        if (instructions == null || instructions.isBlank()
                || instructions.length() > 10000) {
            throw new IllegalArgumentException(
                "Valid exercise instructions are required"
            );
        }

        if (starterRepositoryUrl == null
                || starterRepositoryUrl.isBlank()
                || starterRepositoryUrl.length() > 2048) {
            throw new IllegalArgumentException(
                "Starter repository URL is required"
            );
        }

        this.instructions = instructions;
        this.starterRepositoryUrl = starterRepositoryUrl;
        this.deadline = deadline;
        this.allowLateSubmission = allowLateSubmission;
    }

    public String getInstructions() {
        return instructions;
    }

    public String getStarterRepositoryUrl() {
        return starterRepositoryUrl;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public boolean isAllowLateSubmission() {
        return allowLateSubmission;
    }
}