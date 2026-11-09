
package no.hvl.dat250.lms.content.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SubmitQuizRequest(

        @NotEmpty
        List<@Valid Answer> answers

) {

    public record Answer(

            @NotNull
            @Positive
            Long questionId,

            @NotNull
            List<@NotNull @Positive Long> selectedOptionIds

    ) {}
}
