package no.hvl.dat250.lms.group.repository;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import no.hvl.dat250.lms.group.domain.GroupMatchmaking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface GroupMatchmakingRepository extends JpaRepository<GroupMatchmaking, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from GroupMatchmaking m where m.courseId = :courseId")
    Optional<GroupMatchmaking> lockByCourseId(Long courseId);

    @Query("select m.courseId from GroupMatchmaking m where m.completed = false and m.deadline <= :now")
    List<Long> findDueCourses(Instant now);
}
