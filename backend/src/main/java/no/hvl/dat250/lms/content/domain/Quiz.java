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

    protected Quiz(){}

    public int getMaxAttempts(){
        return maxAttempts;
    }


}
