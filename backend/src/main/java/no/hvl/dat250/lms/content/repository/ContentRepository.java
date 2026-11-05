package no.hvl.dat250.lms.content.repository;

import no.hvl.dat250.lms.content.domain.Content;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentRepository
        extends JpaRepository<Content, Long> {

    List<Content> findByModule_IdOrderByPositionAsc(Long moduleId);
}
