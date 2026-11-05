package no.hvl.dat250.lms.module.repository;

import java.util.List;

import no.hvl.dat250.lms.module.domain.Module;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModuleRepository
        extends JpaRepository<Module, Long> {

    List<Module> findByCourse_IdOrderByPositionAsc(Long courseId);
}