
package no.hvl.dat250.lms.content.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SubmitQuizRequest(

        @NotEmpty
        List<@NotNull @Valid Answer> answers

) {

    public record Answer(

            @NotNull
            @Positive
            Long questionId,

            List<@NotNull @Positive Long> selectedOptionIds,

            @Size(max = 255)
            String textAnswer

    ) {}
}
