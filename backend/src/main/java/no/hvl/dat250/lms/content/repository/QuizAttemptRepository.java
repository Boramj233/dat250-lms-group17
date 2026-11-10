package no.hvl.dat250.lms.content.repository;

import java.util.List;

import no.hvl.dat250.lms.content.domain.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizAttemptRepository
        extends JpaRepository<QuizAttempt, Long> {

    long countByEnrollment_IdAndQuiz_Id(
        Long enrollmentId,
        Long quizId
    );

    List<QuizAttempt> findByEnrollment_IdAndQuiz_IdOrderBySubmittedAtDesc(
        Long enrollmentId,
        Long quizId
    );
}