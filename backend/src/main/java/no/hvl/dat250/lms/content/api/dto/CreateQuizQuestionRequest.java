package no.hvl.dat250.lms.content.api.dto;

import java.util.List;
import no.hvl.dat250.lms.content.domain.QuestionType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.AssertTrue;
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

    List<@NotNull @Valid OptionInput> options,

    QuestionType type,

    @Size(max = 20)
    List<@NotBlank @Size(max = 255) String> acceptedAnswers
) {
    public CreateQuizQuestionRequest {
        if (type == null) {
            type = QuestionType.MULTIPLE_CHOICE;
        }
    }

    @AssertTrue(message = "Choice questions need at least two options; fill-in questions must not contain options")
    public boolean isOptionsValid() {
        if (type == QuestionType.FILL_IN_BLANK) {
            return options == null || options.isEmpty();
        }
        return options != null && options.size() >= 2;
    }

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