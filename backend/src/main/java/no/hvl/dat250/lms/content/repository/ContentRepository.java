package no.hvl.dat250.lms.content.repository;

import no.hvl.dat250.lms.content.domain.Content;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentRepository
        extends JpaRepository<Content, Long> {

    List<Content> findByModule_IdOrderByPositionAsc(Long moduleId);

    boolean existsByModule_IdAndPosition(Long moduleId, int position);

    @Override
    @EntityGraph(attributePaths = {"module", "module.course"})
    Optional<Content> findById(Long id);

}
