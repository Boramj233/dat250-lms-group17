package no.hvl.dat250.lms.group.repository;

import no.hvl.dat250.lms.group.domain.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {

    List<Group> findByCourseId(Long courseId);

    Optional<Group> findByIdAndCourseId(Long groupId, Long courseId);

    boolean existsByCourseIdAndGroupNumber (Long courseId, Integer groupNumber);

    // TODO: Mockito cannot verify that this query works against a database\
    // create a separate DataJpaTest
    @Query("""
        SELECT CASE WHEN COUNT(g) > 0 THEN true ELSE false END
        FROM Group g
        JOIN g.members m
        WHERE g.course.id = :courseId
        AND m.id = :userId
    """)
    boolean existsUserInCourseGroup(
            @Param("courseId") Long courseId,
            @Param("userId") Long userId
    );
}
