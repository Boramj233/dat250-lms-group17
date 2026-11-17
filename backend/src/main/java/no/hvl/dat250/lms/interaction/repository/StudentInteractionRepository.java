package no.hvl.dat250.lms.interaction.repository;

import no.hvl.dat250.lms.interaction.domain.StudentInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StudentInteractionRepository
        extends JpaRepository<StudentInteraction, Long> {

    Optional<StudentInteraction> findByStudent_UserIdAndContent_Id(
            Long studentId,
            Long contentId
    );

    // This query counts incomplete contents that are required for a module
    // to be marked as complete
    @Query("""
        SELECT COUNT(c)
        FROM Content c
        WHERE c.module.id = :moduleId
            AND c.requiredForModuleCompletion = true
            AND NOT EXISTS (
                SELECT si.id
                FROM StudentInteraction si
                WHERE si.content = c
                    AND si.student.userId = :studentId
                    AND si.completedAt IS NOT NULL
                )
        """)
    long countIncompleteRequiredContent(
            @Param("moduleId") Long moduleId,
            @Param("studentId") Long studentId
    );
}
