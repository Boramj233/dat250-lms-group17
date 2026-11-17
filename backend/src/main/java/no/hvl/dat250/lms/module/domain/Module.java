package no.hvl.dat250.lms.module.domain;

import jakarta.persistence.*;
import no.hvl.dat250.lms.content.domain.Content;
import no.hvl.dat250.lms.course.domain.Course;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "modules")
public class Module {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "available_from")
    private LocalDateTime availableFrom;

    @Column(name = "available_to_all_from")
    private LocalDateTime availableToAllFrom;

    @Column(name = "module_position", nullable = false)
    private int position;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @OneToMany(mappedBy = "module")
    @OrderBy("position ASC")
    private List<Content> contents = new ArrayList<>();

    // Modules that must be completed before this module
    @ManyToMany
    @JoinTable(
            name = "module_dependencies",
            joinColumns = @JoinColumn(name = "module_id"),
            inverseJoinColumns = @JoinColumn(name = "required_module_id")
    )
    private Set<Module> dependencies = new HashSet<>();

    protected Module() {}

    public Module(Course course, String title, int position) {
        this.course = course;
        this.title = title;
        this.position = position;
    }

    public Long getId() {
        return id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public LocalDateTime getAvailableFrom() {
        return availableFrom;
    }

    public LocalDateTime getAvailableToAllFrom() {
        return availableToAllFrom;
    }

    public void setAvailability(
            LocalDateTime availableFrom,
            LocalDateTime availableToAllFrom) {

        if (availableFrom != null
                && availableToAllFrom != null
                && availableToAllFrom.isBefore(availableFrom)) {
            throw new IllegalArgumentException(
                    "availableToAllFrom cannot be before availableFrom"
            );
        }

        this.availableFrom = availableFrom;
        this.availableToAllFrom = availableToAllFrom;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getPosition() {
        return position;
    }

    public Course getCourse() {
        return course;
    }

    public List<Content> getContents() {
        return Collections.unmodifiableList(contents);
    }

    public Set<Module> getDependencies() {
        return Collections.unmodifiableSet(dependencies);
    }

    public void addDependency(Module module) {
        dependencies.add(module);
    }

    public void removeDependency(Module module) {
        dependencies.remove(module);
    }
}
