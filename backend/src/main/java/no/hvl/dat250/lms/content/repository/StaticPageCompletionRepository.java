package no.hvl.dat250.lms.content.repository;
import no.hvl.dat250.lms.content.domain.StaticPageCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
public interface StaticPageCompletionRepository extends JpaRepository<StaticPageCompletion, Long> {
    boolean existsByContent_Id(Long contentId);
    boolean existsByContent_IdAndEnrollment_Id(Long contentId, Long enrollmentId);
    void deleteByContent_IdAndEnrollment_Id(Long contentId, Long enrollmentId);
}
