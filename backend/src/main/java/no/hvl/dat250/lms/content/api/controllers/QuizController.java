
package no.hvl.dat250.lms.content.api.controllers;

import java.util.List;
import java.util.NoSuchElementException;

import jakarta.validation.Valid;

import no.hvl.dat250.lms.content.api.dto.CreateQuizRequest;
import no.hvl.dat250.lms.content.api.dto.QuizDetailResponse;
import no.hvl.dat250.lms.content.api.dto.CreateQuizQuestionRequest;
import no.hvl.dat250.lms.content.api.dto.QuizResponse;
import no.hvl.dat250.lms.content.api.dto.QuizQuestionResponse;

import no.hvl.dat250.lms.content.domain.Quiz;
import no.hvl.dat250.lms.content.domain.QuizQuestion;
import no.hvl.dat250.lms.content.service.QuizService;
import no.hvl.dat250.lms.content.service.QuizService.OptionDraft;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @PostMapping(
        "/api/courses/{courseId}/modules/{moduleId}/contents/quizzes"
    )
    public ResponseEntity<QuizResponse> createQuiz(
            @PathVariable Long courseId,
            @PathVariable Long moduleId,
            @Valid @RequestBody CreateQuizRequest request) {

        Quiz quiz = quizService.createQuiz(
            courseId,
            moduleId,
            request.title(),
            request.position(),
            request.maxAttempts()
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(QuizResponse.from(
                quiz, courseId, moduleId
            ));
    }

    @PostMapping("/api/quizzes/{quizId}/questions")
    public ResponseEntity<QuizQuestionResponse> addQuestion(
            @PathVariable Long quizId,
            @Valid @RequestBody CreateQuizQuestionRequest request) {

        List<OptionDraft> options = request.options()
            .stream()
            .map(option -> new OptionDraft(
                option.optionText(),
                option.position(),
                option.correct()
            ))
            .toList();

        QuizQuestion question = quizService.addQuestion(
            quizId,
            request.questionText(),
            request.position(),
            request.hint(),
            options
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(QuizQuestionResponse.from(
                question, quizId
            ));
    }

    @GetMapping("/api/quizzes/{quizId}")
    public ResponseEntity<QuizDetailResponse> getQuiz(
            @PathVariable Long quizId) {

        var details = quizService.findQuizWithQuestions(quizId);

        return ResponseEntity.ok(
            QuizDetailResponse.from(details)
        );
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Void> handleNotFound(
            NoSuchElementException exception) {

        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Void> handleBadRequest(
            IllegalArgumentException exception) {

        return ResponseEntity.badRequest().build();
    }
}
