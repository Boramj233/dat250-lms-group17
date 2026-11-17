package no.hvl.dat250.lms.content.repository;

import java.util.List;
import java.util.Optional;

import no.hvl.dat250.lms.content.domain.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizAttemptRepository
        extends JpaRepository<QuizAttempt, Long> {

    @org.springframework.data.jpa.repository.Query("""
        select count(a) from QuizAttempt a where a.quiz.id = :quizId and a.enrollment.id = :enrollmentId
        and a.correctCount * 100L >= :percentage * a.totalQuestions
        """)
    long countPassing(Long quizId, Long enrollmentId, int percentage);

    boolean existsByQuiz_Id(Long quizId);

    long countByEnrollment_IdAndQuiz_Id(
        Long enrollmentId,
        Long quizId
    );

    Optional<QuizAttempt> findByIdAndQuiz_IdAndEnrollment_Id(Long id, Long quizId, Long enrollmentId);

    // ID breaks ties when attempts have the same submission timestamp.
    @org.springframework.data.jpa.repository.Query("""
        select a from QuizAttempt a where a.enrollment.id = :enrollmentId and a.quiz.id = :quizId
        order by a.submittedAt desc, a.id desc
        """)
    List<QuizAttempt> findByEnrollment_IdAndQuiz_IdOrderBySubmittedAtDesc(
        Long enrollmentId,
        Long quizId
    );
}