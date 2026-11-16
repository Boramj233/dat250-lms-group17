package no.hvl.dat250.lms.grade.service;

import no.hvl.dat250.lms.grade.repository.GradeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only completion evidence; deliberately independent of submission orchestration. */
@Service
public class GradeCompletionService {
    private final GradeRepository grades;
    public GradeCompletionService(GradeRepository grades) { this.grades = grades; }
    @Transactional(readOnly = true)
    public boolean hasReleasedPass(Long contentId, Long userId) {
        return grades.countReleasedPassed(contentId, userId) > 0;
    }
}
