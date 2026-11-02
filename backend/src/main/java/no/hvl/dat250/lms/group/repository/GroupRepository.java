package no.hvl.dat250.lms.group.repository;

import no.hvl.dat250.lms.group.domain.Group;

import java.util.List;

public interface GroupRepository extends JPARepository<Group, Long> {

    List<Group> findByCourseId(Long courseId);
}
