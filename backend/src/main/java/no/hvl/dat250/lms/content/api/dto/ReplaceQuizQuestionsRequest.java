package no.hvl.dat250.lms.content.api.dto;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Every question of a quiz, replacing the ones it has: PUT /api/quizzes/{quizId}/questions.
 * Each question is checked like POST /api/quizzes/{quizId}/questions. Refused (409) once
 * a student has made an attempt, like adding a question.
 */
public record ReplaceQuizQuestionsRequest(
    @NotNull @Size(max = 100) List<@NotNull @Valid CreateQuizQuestionRequest> questions
) {}
