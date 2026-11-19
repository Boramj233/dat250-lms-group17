package no.hvl.dat250.lms.group.domain;

import jakarta.persistence.*;
import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.user.domain.User;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
    name = "groups",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"course_id", "group_number"})
    }
)
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_number", nullable = false)
    private Integer groupNumber;

    @Column(name = "max_group_size")
    private Integer maxGroupSize;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToMany
    @JoinTable(
            name = "group_members",
            joinColumns = @JoinColumn(
                    name = "group_id",
                    nullable = false
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "user_id",
                    nullable = false
            ),
            uniqueConstraints = @UniqueConstraint(
                    columnNames = {"group_id", "user_id"}
            )
    )
    private final Set<User> members = new HashSet<>();

    protected Group() {} // JPA needs this

    public Group(int groupNumber, Course course, Integer maxGroupSize) {
        this.groupNumber = groupNumber;
        this.course = course;

        if (maxGroupSize != null && maxGroupSize < 1) {
            throw new IllegalArgumentException(
                    "Maximum group size must be positive"
            );
        }

        this.maxGroupSize = maxGroupSize;
    }

    public Long getGroupId() {
        return this.id;
    }

    public int getGroupNumber() { return this.groupNumber; }

    public Course getCourse() {
        return this.course;
    }

    public Integer getMaxGroupSize() { return this.maxGroupSize; }

    public Set<User> getGroupMembers() {
        return this.members;
    }

    public void addGroupMember(User user) {

        if (members.contains(user)) {
            return;
        }

        if (maxGroupSize != null
                && members.size() >= maxGroupSize) {
            throw new IllegalStateException(
                    "Group has reached maximum capacity"
            );
        }

        members.add(user);
    }

    public void removeGroupMember(User user) {
        members.remove(user);
    }
}
