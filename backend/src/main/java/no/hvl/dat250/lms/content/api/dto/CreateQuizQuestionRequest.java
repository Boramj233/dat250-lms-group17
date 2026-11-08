package no.hvl.dat250.lms.content.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateQuizQuestionRequest(
    @NotBlank
    String questionText,

    @NotNull
    @Min(0)
    Integer position,

    String hint,

    @NotNull
    @Size(min = 2)
    List<@NotNull @Valid OptionInput> options
) {
    public record OptionInput(
        @NotBlank
        String optionText,

        @NotNull
        @Min(0)
        Integer position,

        @NotNull
        Boolean correct
    ) {}
}