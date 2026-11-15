package no.hvl.dat250.lms.interaction.repository;

import no.hvl.dat250.lms.interaction.domain.StudentInteraction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentInteractionRepository
        extends JpaRepository<StudentInteraction, Long> {

    Optional<StudentInteraction> findByStudentIdAndContentId(
            Long studentId,
            Long contentId
    );

    List<StudentInteraction> findByStudentId(
           Long studentId
    );

    boolean existsByStudentIdAndContentIdAndCompletedAtIsNotNull(
            Long studentId,
            Long contentId
    );
}
