package no.hvl.dat250.lms.grade.repository;

import java.util.Optional;
import no.hvl.dat250.lms.grade.domain.Grade;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GradeRepository extends JpaRepository<Grade, Long> {
    Optional<Grade> findBySubmission_Id(Long submissionId);
}
