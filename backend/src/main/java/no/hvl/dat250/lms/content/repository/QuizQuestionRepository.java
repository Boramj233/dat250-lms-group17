package no.hvl.dat250.lms.content.repository;

import java.util.List;

import no.hvl.dat250.lms.content.domain.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizQuestionRepository
        extends JpaRepository<QuizQuestion, Long> {

    List<QuizQuestion> findByQuiz_IdOrderByPositionAsc(
            Long quizId);
}