package no.hvl.dat250.lms.content.repository;

import java.util.List;

import no.hvl.dat250.lms.content.domain.QuizAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizAnswerRepository
        extends JpaRepository<QuizAnswer, Long> {

    @org.springframework.data.jpa.repository.Query("""
        select a from QuizAnswer a where a.attempt.id = :attemptId
        order by a.question.position asc, a.question.id asc
        """)
    List<QuizAnswer> findByAttempt_IdOrderByQuestion_PositionAsc(
        Long attemptId
    );
}