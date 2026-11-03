package no.hvl.dat250.lms.course.api.controllers;

import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import no.hvl.dat250.lms.course.api.dto.CourseResponse;
import no.hvl.dat250.lms.course.api.dto.CreateCourseRequest;
import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.service.CourseService;

@RestController
@RequestMapping ("/api/courses")
public class CourseController {

    private final CourseService service;

    public CourseController(CourseService service){
        this.service = service;
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> getCourse(
        @PathVariable Long id
    ){
        Optional<Course> found = service.findCourseById(id);

        if (found.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        CourseResponse response =
            CourseResponse.from(found.get());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping
    public ResponseEntity<CourseResponse> createCourse(
        @Valid @RequestBody CreateCourseRequest request
    ){
        Course saved = service.createCourse(
            request.title(),
            request.description(),
            request.term(),
            request.year()
        );

        CourseResponse response = CourseResponse.from(saved);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

}
