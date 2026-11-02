package no.hvl.dat250.lms.group.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "groups")
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    // A group consists of 0-n members, which of all members must be students
    @Column(unique = true, nullable = true)
    private String members;

    @Column(unique = true, nullable = false)
    private int groupNumber;

    public Group() {}
}
