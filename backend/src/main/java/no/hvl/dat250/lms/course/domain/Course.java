package no.hvl.dat250.lms.course.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Term term;

    @Column(name = "academic_year", nullable = false)
    private int year;

    protected Course() {
        // Required by JPA
    }

    public Course(
            String title,
            String description,
            Term term,
            int year
    ) {
        this.title = title;
        this.description = description;
        this.term = term;
        this.year = year;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Term getTerm() {
        return term;
    }

    public int getYear() {
        return year;
    }
}