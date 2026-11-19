package no.hvl.dat250.lms.group.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import no.hvl.dat250.lms.course.domain.Course;

@Entity
@Table(name = "group_matchmaking")
public class GroupMatchmaking {
    @Id
    private Long courseId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(nullable = false)
    private Instant deadline;

    @Column(nullable = false)
    private int maxGroupSize;

    @Column(nullable = false)
    private boolean completed;

    @ElementCollection
    @CollectionTable(name = "group_matchmaking_preferences", joinColumns = @JoinColumn(name = "course_id"))
    @MapKeyColumn(name = "user_id")
    private Map<Long, MatchmakingPreference> preferences = new HashMap<>();

    protected GroupMatchmaking() {}

    public GroupMatchmaking(Course course, Instant deadline, int maxGroupSize) {
        this.courseId = course.getId();
        this.course = course;
        this.deadline = deadline;
        this.maxGroupSize = maxGroupSize;
    }

    public Course getCourse() { return course; }
    public Instant getDeadline() { return deadline; }
    public int getMaxGroupSize() { return maxGroupSize; }
    public boolean isCompleted() { return completed; }
    public Map<Long, MatchmakingPreference> preferences() { return Map.copyOf(preferences); }

    public void submit(Long userId, MatchmakingPreference preference) {
        preferences.put(userId, preference);
    }

    public void complete() {
        completed = true;
    }
}
