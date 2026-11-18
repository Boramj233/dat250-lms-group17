package no.hvl.dat250.lms.content.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import no.hvl.dat250.lms.module.domain.Module;

@Entity
@Table(name = "quizzes")
public class Quiz extends Content {

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    public Quiz(
        Module module,
        String title,
        int position,
        int maxAttempts
    ){
        super(module, title, position);

        if (maxAttempts < 1) {
            throw new IllegalArgumentException(
                "Max attempts must be at least 1");
        }

        this.maxAttempts = maxAttempts;
    }

    @Column(name = "passing_percentage", nullable = false)
    private int passingPercentage = 60;

    public int getPassingPercentage() { return passingPercentage; }

    public void setPassingPercentage(int percentage) {
        if (percentage < 1 || percentage > 100) {
            throw new IllegalArgumentException("Passing percentage must be between 1 and 100");
        }
        passingPercentage = percentage;
    }

    protected Quiz(){}

    public int getMaxAttempts(){
        return maxAttempts;
    }

    /** Attempts made before stay; lowering it only stops new attempts beyond the limit. */
    public void changeMaxAttempts(int maxAttempts) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("Max attempts must be at least 1");
        }
        this.maxAttempts = maxAttempts;
    }


}
