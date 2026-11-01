package no.hvl.dat250.lms.user.domain;

/** Global access levels, separate from a user's role in a specific course. */
public enum Access {
    /** Default access for ordinary users. */
    STANDARD,
    /** Access intended for users who may create courses. */
    COURSE_MANAGER,
    /** Access intended for application administrators. */
    ADMIN
}
