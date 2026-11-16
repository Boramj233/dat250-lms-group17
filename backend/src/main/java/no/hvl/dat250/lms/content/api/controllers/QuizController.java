
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
import no.hvl.dat250.lms.content.service.QuizService.ItemDraft;
import no.hvl.dat250.lms.content.service.QuizService.OrderingDraft;
import no.hvl.dat250.lms.content.service.QuizService.MatchingDraft;
import no.hvl.dat250.lms.content.service.QuizService.MatchDraft;

import no.hvl.dat250.lms.content.service.QuizQuestionConflictException;
import no.hvl.dat250.lms.security.auth.annotations.IsTeacherInCourse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @IsTeacherInCourse
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

    @PreAuthorize("@courseAccess.isTeacher(authentication, @quizService.courseId(#quizId))")
    @PostMapping("/api/quizzes/{quizId}/questions")
    public ResponseEntity<QuizQuestionResponse> addQuestion(
            @PathVariable Long quizId,
            @Valid @RequestBody CreateQuizQuestionRequest request) {

        List<OptionDraft> options = request.options() == null ? List.of() : request.options()
            .stream()
            .map(option -> new OptionDraft(
                option.optionText(),
                option.position(),
                option.correct()
            ))
            .toList();

        QuizQuestion question;
        if (request.matching() != null) {
            if (request.ordering() != null) {
                throw new IllegalArgumentException("Ordering and matching definitions cannot be combined");
            }
            MatchingDraft matching = new MatchingDraft(
                request.matching().leftItems().stream().map(item -> new ItemDraft(item.key(), item.text())).toList(),
                request.matching().rightItems().stream().map(item -> new ItemDraft(item.key(), item.text())).toList(),
                request.matching().correctMatches().stream()
                    .map(pair -> new MatchDraft(pair.leftKey(), pair.rightKey())).toList());
            question = quizService.addQuestion(quizId, request.questionText(), request.position(),
                request.hint(), options, request.type(), request.acceptedAnswers(), null, matching);
        } else if (request.ordering() == null) {
            question = quizService.addQuestion(
                quizId,
                request.questionText(),
                request.position(),
                request.hint(),
                options,
                request.type(),
                request.acceptedAnswers() == null ? List.of() : request.acceptedAnswers()
            );
        } else {
            OrderingDraft ordering = new OrderingDraft(request.ordering().items().stream()
                .map(item -> new ItemDraft(item.key(), item.text())).toList(), request.ordering().correctOrder());
            question = quizService.addQuestion(quizId, request.questionText(), request.position(),
                request.hint(), options, request.type(), request.acceptedAnswers(), ordering);
        }

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(QuizQuestionResponse.from(
                question, quizId
            ));
    }

    @PreAuthorize("@courseAccess.isEnrolled(authentication, @quizService.courseId(#quizId))")
    @GetMapping("/api/quizzes/{quizId}")
    public ResponseEntity<QuizDetailResponse> getQuiz(
            @PathVariable Long quizId) {

        var details = quizService.findQuizWithQuestions(quizId);

        return ResponseEntity.ok(
            QuizDetailResponse.from(details)
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Void> handleForbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @ExceptionHandler(QuizQuestionConflictException.class)
    public ResponseEntity<Void> handleQuestionConflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
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
