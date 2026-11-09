package no.hvl.dat250.lms.group.domain;

import jakarta.persistence.*;
import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.user.domain.User;

import java.util.HashSet;
import java.util.Set;

/**
 * Group domain represents the database entity
 */

@Entity
@Table(name = "groups")
public class Group {

    /**
     * conflicted on how to store groups, but for the time being the vision is to let groups be tied
     * to courses instead of modules;
     * Thus, a group should be stored with the corresponding course, it's group number, and it's members;
     * This way, the identity can simply be {group_number, course};
     * Why then opt for an id? -> Easier to tie the groups to a module later on;
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer groupNumber;

    private Course course;

    private Set<User> members = new HashSet<>();

    protected Group() {} // JPA needs this

    public Group(int groupNumber, Course course) {
        this.groupNumber = groupNumber;
        this.course = course;
    }

    public Long getGroupId() {
        return this.id;
    }

    public int getGroupNumber() {
        return this.groupNumber;
    }

    public Course getCourse() {
        return this.course;
    }

    public Set<User> getGroupMembers() {
        return this.members;
    }

    public void addGroupMember(User user) {
        members.add(user);
    }

    public void removeGroupMember(User user) {
        members.remove(user);
    }
}
