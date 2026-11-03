package no.hvl.dat250.lms.course.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import no.hvl.dat250.lms.course.domain.Term;

public record CreateCourseRequest(

    @NotBlank
    String title,

    @NotBlank
    String description,

    @NotNull
    Term term,

    @Min(1)
    int year

) {}
