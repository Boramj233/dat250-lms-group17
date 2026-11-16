
package no.hvl.dat250.lms.content.api.dto;

import java.util.List;
import java.util.UUID;

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
            String textAnswer,

            @Size(max = 20)
            List<@NotNull UUID> orderedItemIds

    ) {
        public Answer(Long questionId, List<Long> selectedOptionIds, String textAnswer) {
            this(questionId, selectedOptionIds, textAnswer, null);
        }
    }
}
