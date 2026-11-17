
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

    @Column(name = "starter_code", length = 10000)
    private String starterCode;

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

        this(module, title, position, instructions, starterRepositoryUrl, null,
            deadline, allowLateSubmission);
    }

    public CodingExercise(
            Module module,
            String title,
            int position,
            String instructions,
            String starterRepositoryUrl,
            String starterCode,
            Instant deadline,
            boolean allowLateSubmission) {

        super(module, title, position);

        validateDetails(instructions, starterRepositoryUrl, starterCode);

        this.starterCode = starterCode;
        this.instructions = instructions;
        this.starterRepositoryUrl = starterRepositoryUrl;
        this.deadline = deadline;
        this.allowLateSubmission = allowLateSubmission;
    }

    private static void validateDetails(String instructions, String starterRepositoryUrl, String starterCode) {
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

        if (starterCode != null && starterCode.length() > 10000) {
            throw new IllegalArgumentException(
                "Starter code must not exceed 10000 characters"
            );
        }

    }

    public void updateDetails(String title, String instructions, String starterRepositoryUrl, String starterCode) {
        validateDetails(instructions, starterRepositoryUrl, starterCode);
        changeTitle(title);
        this.instructions = instructions;
        this.starterRepositoryUrl = starterRepositoryUrl;
        this.starterCode = starterCode;
    }

    public String getInstructions() {
        return instructions;
    }

    public String getStarterRepositoryUrl() {
        return starterRepositoryUrl;
    }

    public String getStarterCode() {
        return starterCode;
    }

    public void updateSubmissionSettings(Instant deadline, boolean allowLateSubmission) {
        this.deadline = deadline;
        this.allowLateSubmission = allowLateSubmission;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public boolean isAllowLateSubmission() {
        return allowLateSubmission;
    }
}