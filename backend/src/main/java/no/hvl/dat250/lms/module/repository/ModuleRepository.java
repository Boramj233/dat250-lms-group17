package no.hvl.dat250.lms.module.repository;

import java.util.List;
import java.util.Optional;

import no.hvl.dat250.lms.module.domain.Module;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModuleRepository extends JpaRepository<Module, Long> {

    List<Module> findByCourse_IdOrderByPositionAsc(Long courseId);

    Optional<Module> findByIdAndCourse_Id(Long moduleId, Long courseId);
}