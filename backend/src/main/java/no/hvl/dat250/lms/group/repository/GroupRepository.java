package no.hvl.dat250.lms.group.repository;

import no.hvl.dat250.lms.group.domain.Group;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {

    List<Group> findByCourseId(Long courseId);

    Optional<Group> findByIdAndCourseId(Long groupId, Long courseId);
}
