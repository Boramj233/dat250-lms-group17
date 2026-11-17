
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

    @Column(name = "mandatory_for_exam", nullable = false)
    private boolean mandatoryForExam;

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

    public boolean isMandatoryForExam() {
        return mandatoryForExam;
    }

    public void setMandatoryForExam(boolean mandatoryForExam) {
        this.mandatoryForExam = mandatoryForExam;
    }

    public void updateDetails(String title, String instructions) {
        if (instructions == null || instructions.isBlank() || instructions.length() > 10000) {
            throw new IllegalArgumentException("Instructions must be nonblank and at most 10000 characters");
        }
        changeTitle(title);
        this.instructions = instructions;
    }

    public String getInstructions() {
        return instructions;
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
