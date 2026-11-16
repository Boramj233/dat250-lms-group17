package no.hvl.dat250.lms.content;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import no.hvl.dat250.lms.content.domain.*;
import no.hvl.dat250.lms.content.service.*;
import no.hvl.dat250.lms.course.domain.*;
import no.hvl.dat250.lms.enrollment.domain.*;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.grade.domain.Grade;
import no.hvl.dat250.lms.grade.service.GradingService;
import no.hvl.dat250.lms.submission.domain.*;
import no.hvl.dat250.lms.group.domain.Group;
import no.hvl.dat250.lms.security.TestTokens;
import no.hvl.dat250.lms.user.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:completion_it;DB_CLOSE_DELAY=-1", "spring.jpa.open-in-view=false"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ContentCompletionIntegrationTest {
    @Autowired EntityManager em;
    @Autowired ContentService contentService;
    @Autowired ContentCompletionService completion;
    @Autowired GradingService grading;
    @Autowired MockMvc mvc;
    <T> T save(T value) { em.persist(value); em.flush(); return value; }
    User user() { return save(new User(UUID.randomUUID()+"@example.com", "Test", "User")); }
    record Fixture(User student, User teacher, Enrollment enrollment, Enrollment teacherEnrollment, Module module) {}
    Fixture fixture() {
        User student=user(), teacher=user();
        Course course=save(new Course("DAT250", "Completion", Term.AUTUMN, 2026));
        Enrollment enrollment=save(new Enrollment(student, course, CourseRole.STUDENT));
        Enrollment teacherEnrollment=save(new Enrollment(teacher, course, CourseRole.TEACHER));
        return new Fixture(student, teacher, enrollment, teacherEnrollment, save(new Module(course,"Module",0)));
    }
    private Content gradedContent(Fixture fixture, boolean coding) {
        return coding
            ? save(new CodingExercise(fixture.module(), "Code", 0, "Work", "https://example.com/repo", null, false))
            : save(new Assignment(fixture.module(), "Assignment", 0, "Work", null, false));
    }
    @Test void pagesPersistAndAreIndependentOfRequiredFlag() {
        var f=fixture(); var page=save(new StaticPage(f.module(),"Page",0,"Body")); Long id=page.getId();
        assertFalse(contentService.isCompleted(id,f.student().getUserId()));
        completion.markPage(id,f.student().getUserId(),true);
        completion.markPage(id,f.student().getUserId(),true);
        em.flush(); em.clear();
        assertTrue(contentService.isCompleted(id,f.student().getUserId()));
        completion.configure(id,f.teacher().getUserId(),true,null);
        assertTrue(completion.status(id,f.student().getUserId()).requiredForModuleCompletion());
        completion.markPage(id,f.student().getUserId(),false);
        completion.markPage(id,f.student().getUserId(),false);
        assertFalse(contentService.isCompleted(id,f.student().getUserId()));
    }
    @Test void quizUsesAnyAttemptAndCurrentExactThreshold() {
        var f=fixture(); var quiz=save(new Quiz(f.module(),"Quiz",0,5));
        assertFalse(contentService.isCompleted(quiz.getId(),f.student().getUserId()));
        save(new QuizAttempt(quiz,f.enrollment(),Instant.now(),2,3));
        assertTrue(contentService.isCompleted(quiz.getId(),f.student().getUserId()));
        completion.configure(quiz.getId(),f.teacher().getUserId(),true,67);
        assertFalse(contentService.isCompleted(quiz.getId(),f.student().getUserId()));
        save(new QuizAttempt(quiz,f.enrollment(),Instant.now(),3,3));
        save(new QuizAttempt(quiz,f.enrollment(),Instant.now(),0,3));
        assertTrue(contentService.isCompleted(quiz.getId(),f.student().getUserId()));
        assertThrows(IllegalArgumentException.class,()->quiz.setPassingPercentage(0));
        assertThrows(IllegalArgumentException.class,()->quiz.setPassingPercentage(101));
    }
    @ParameterizedTest(name = "coding={0}")
    @ValueSource(booleans = {false, true})
    void gradedContentRequiresExplicitReleasedPassRegardlessOfScore(boolean coding) {
        var f=fixture(); var a=gradedContent(f,coding);
        assertFalse(contentService.isCompleted(a.getId(),f.student().getUserId()));
        var s=save(new Submission(a,f.enrollment(),SubmissionType.TEXT,"Answer",Instant.now(),false));
        assertFalse(contentService.isCompleted(a.getId(),f.student().getUserId()));
        var g=save(new Grade(s,new BigDecimal("100"),null,f.teacherEnrollment(),Instant.now()));
        assertFalse(contentService.isCompleted(a.getId(),f.student().getUserId()));
        g.decidePassed(false); g.release(f.teacherEnrollment(),Instant.now()); em.flush();
        assertFalse(contentService.isCompleted(a.getId(),f.student().getUserId()));
        var s2=save(new Submission(a,f.enrollment(),SubmissionType.TEXT,"Second",Instant.now(),false));
        var draft=grading.save(s2.getId(),f.teacher().getUserId(),BigDecimal.ZERO,"Pass despite score",null,true);
        assertEquals(Boolean.TRUE,draft.passed());
        assertFalse(contentService.isCompleted(a.getId(),f.student().getUserId()));
        grading.release(s2.getId(),f.teacher().getUserId(),draft.version());
        assertTrue(contentService.isCompleted(a.getId(),f.student().getUserId()));
        var third=save(new Submission(a,f.enrollment(),SubmissionType.TEXT,"Third",Instant.now(),false));
        assertTrue(contentService.isCompleted(a.getId(),f.student().getUserId()));
        var failed=grading.save(third.getId(),f.teacher().getUserId(),new BigDecimal("100"),"Failed resubmission",null,false);
        grading.release(third.getId(),f.teacher().getUserId(),failed.version());
        em.flush(); em.clear();
        assertTrue(contentService.isCompleted(a.getId(),f.student().getUserId()));
        assertThrows(IllegalStateException.class,()->g.decidePassed(true));
    }
    @ParameterizedTest(name = "coding={0}")
    @ValueSource(booleans = {false, true})
    void undecidedDraftCannotBeReleased(boolean coding) {
        var f=fixture(); var a=gradedContent(f,coding);
        assertFalse(contentService.isCompleted(a.getId(),f.student().getUserId()));
        var s=save(new Submission(a,f.enrollment(),SubmissionType.TEXT,"Answer",Instant.now(),false));
        assertFalse(contentService.isCompleted(a.getId(),f.student().getUserId()));
        var g=save(new Grade(s,new BigDecimal("100"),null,f.teacherEnrollment(),Instant.now()));
        assertThrows(IllegalArgumentException.class, () -> g.release(f.teacherEnrollment(),Instant.now()));
        assertNull(g.getReleasedAt());
        assertNull(g.getPassed());
        assertFalse(contentService.isCompleted(a.getId(),f.student().getUserId()));
    }
    @Test void codingCanBeRequiredButCannotBeManuallyCompleted() {
        var f=fixture(); var c=save(new CodingExercise(f.module(),"Code",0,"Work","https://example.com/repo",null,false));
        assertFalse(contentService.isCompleted(c.getId(),f.student().getUserId()));
        completion.configure(c.getId(),f.teacher().getUserId(),true,null);
        assertTrue(completion.status(c.getId(),f.student().getUserId()).requiredForModuleCompletion());
        assertFalse(contentService.isCompleted(c.getId(),f.student().getUserId()));
        assertThrows(IllegalArgumentException.class,()->completion.markPage(c.getId(),f.student().getUserId(),true));
    }
    @ParameterizedTest(name = "coding={0}")
    @ValueSource(booleans = {false, true})
    void groupPassBelongsToSnapshotAndNotCurrentMembership(boolean coding) {
        var f=fixture(); User former=user(), newcomer=user();
        Enrollment formerEnrollment=save(new Enrollment(former,f.module().getCourse(),CourseRole.STUDENT));
        save(new Enrollment(newcomer,f.module().getCourse(),CourseRole.STUDENT));
        Group group=save(new Group(1,f.module().getCourse()));
        group.addGroupMember(f.student()); group.addGroupMember(newcomer);
        var a=gradedContent(f,coding);
        var submission=new Submission(a,f.enrollment(),SubmissionType.TEXT,"Answer",Instant.now(),false);
        submission.assignGroup(group,java.util.List.of(
            new SubmissionMember(f.student().getUserId(),f.enrollment().getId()),
            new SubmissionMember(former.getUserId(),formerEnrollment.getId())));
        save(submission);
        var grade=new Grade(submission,BigDecimal.ZERO,null,f.teacherEnrollment(),Instant.now());
        grade.decidePassed(true); grade.release(f.teacherEnrollment(),Instant.now()); save(grade);
        assertTrue(contentService.isCompleted(a.getId(),f.student().getUserId()));
        assertTrue(contentService.isCompleted(a.getId(),former.getUserId()));
        assertFalse(contentService.isCompleted(a.getId(),newcomer.getUserId()));
    }
    @Test void inactiveStudentsCannotReadOrWriteProgress() {
        var f=fixture(); var page=save(new StaticPage(f.module(),"Page",0,"Body"));
        f.enrollment().deactivate(); em.flush();
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
            ()->completion.status(page.getId(),f.student().getUserId()));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
            ()->completion.markPage(page.getId(),f.student().getUserId(),true));
    }
    @Test void realFiltersProtectSettingsAndStudentIdentity() throws Exception {
        var f=fixture(); var p=save(new StaticPage(f.module(),"Page",0,"Body"));
        String path="/api/contents/"+p.getId();
        mvc.perform(get(path+"/completion")).andExpect(status().isUnauthorized());
        mvc.perform(get(path+"/completion").header("Authorization","Bearer invalid")).andExpect(status().isUnauthorized());
        mvc.perform(patch(path+"/completion-settings").header("Authorization",TestTokens.bearer(f.student()))
            .contentType(MediaType.APPLICATION_JSON).content("{\"requiredForModuleCompletion\":true}"))
            .andExpect(status().isForbidden());
        mvc.perform(patch(path+"/completion-settings").header("Authorization",TestTokens.bearer(f.teacher()))
            .contentType(MediaType.APPLICATION_JSON).content("{\"requiredForModuleCompletion\":true}"))
            .andExpect(status().isNoContent());
        mvc.perform(put(path+"/completion").header("Authorization",TestTokens.bearer(f.student()))
            .contentType(MediaType.APPLICATION_JSON).content("{\"completed\":true,\"userId\":999}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.completed").value(true));
        var outsider=fixture();
        mvc.perform(get(path+"/completion").header("Authorization",TestTokens.bearer(outsider.student())))
            .andExpect(status().isForbidden());
        mvc.perform(patch(path+"/completion-settings").header("Authorization",TestTokens.bearer(outsider.teacher()))
            .contentType(MediaType.APPLICATION_JSON).content("{\"requiredForModuleCompletion\":false}"))
            .andExpect(status().isForbidden());
        mvc.perform(put(path+"/completion").header("Authorization",TestTokens.bearer(f.teacher()))
            .contentType(MediaType.APPLICATION_JSON).content("{\"completed\":true}"))
            .andExpect(status().isForbidden());
    }
    private Content contentOfType(Fixture fixture, String type) {
        return switch (type) {
            case "STATIC_PAGE" -> save(new StaticPage(fixture.module(), "Page", 0, "Body"));
            case "QUIZ" -> save(new Quiz(fixture.module(), "Quiz", 0, 3));
            case "ASSIGNMENT" -> gradedContent(fixture, false);
            case "CODING_EXERCISE" -> gradedContent(fixture, true);
            default -> throw new IllegalArgumentException("Unknown test content type");
        };
    }

    @ParameterizedTest
    @ValueSource(strings = {"STATIC_PAGE", "QUIZ", "ASSIGNMENT", "CODING_EXERCISE"})
    void requiredSettingsPersistWithoutCreatingCompletion(String type) throws Exception {
        var f = fixture();
        Content content = contentOfType(f, type);
        Long id = content.getId();
        assertFalse(content.isRequiredForModuleCompletion());
        String path = "/api/contents/" + id;
        for (boolean required : new boolean[]{true, false}) {
            mvc.perform(patch(path + "/completion-settings")
                    .header("Authorization", TestTokens.bearer(f.teacher()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"requiredForModuleCompletion\":" + required + "}"))
                .andExpect(status().isNoContent());
            em.flush(); em.clear();
            assertEquals(required, em.find(Content.class, id).isRequiredForModuleCompletion());
            mvc.perform(get(path + "/completion").header("Authorization", TestTokens.bearer(f.student())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredForModuleCompletion").value(required))
                .andExpect(jsonPath("$.completed").value(false));
        }
    }

    @Test
    void codingCompletionSettingsAndProgressKeepCoursePermissions() throws Exception {
        var f = fixture();
        Content coding = gradedContent(f, true);
        var other = fixture();
        User inactiveTeacher = user();
        Enrollment inactive = save(new Enrollment(inactiveTeacher, f.module().getCourse(), CourseRole.TEACHER));
        inactive.deactivate(); em.flush();
        String path = "/api/contents/" + coding.getId();
        mvc.perform(patch(path + "/completion-settings").contentType(MediaType.APPLICATION_JSON)
                .content("{\"requiredForModuleCompletion\":true}"))
            .andExpect(status().isUnauthorized());
        for (User denied : java.util.List.of(f.student(), other.teacher(), inactiveTeacher)) {
            mvc.perform(patch(path + "/completion-settings").header("Authorization", TestTokens.bearer(denied))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"requiredForModuleCompletion\":true}"))
                .andExpect(status().isForbidden());
        }
        assertFalse(coding.isRequiredForModuleCompletion());
        mvc.perform(patch(path + "/completion-settings").header("Authorization", TestTokens.bearer(f.teacher()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"requiredForModuleCompletion\":true,\"passingPercentage\":60}"))
            .andExpect(status().isBadRequest());
        assertFalse(coding.isRequiredForModuleCompletion());
        mvc.perform(put(path + "/completion").header("Authorization", TestTokens.bearer(f.student()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"completed\":true}"))
            .andExpect(status().isBadRequest());
        for (User denied : java.util.List.of(f.teacher(), other.student())) {
            mvc.perform(get(path + "/completion").header("Authorization", TestTokens.bearer(denied)))
                .andExpect(status().isForbidden());
        }
    }

    @ParameterizedTest(name = "coding={0}")
    @ValueSource(booleans = {false, true})
    void anotherStudentsReleasedPassDoesNotCompleteContent(boolean coding) {
        var f = fixture();
        Content content = gradedContent(f, coding);
        User other = user();
        Enrollment enrollment = save(new Enrollment(other, f.module().getCourse(), CourseRole.STUDENT));
        var submission = save(new Submission(content, enrollment, SubmissionType.TEXT, "Answer", Instant.now(), false));
        var grade = grading.save(submission.getId(), f.teacher().getUserId(), BigDecimal.ZERO, null, null, true);
        grading.release(submission.getId(), f.teacher().getUserId(), grade.version());
        em.flush(); em.clear();
        assertTrue(contentService.isCompleted(content.getId(), other.getUserId()));
        assertFalse(contentService.isCompleted(content.getId(), f.student().getUserId()));
    }

    @Test
    void serviceContractValidatesIdsAndActiveStudentEnrollment() {
        var f = fixture();
        Content content = gradedContent(f, true);
        Long id = content.getId();
        for (Long invalid : new Long[]{null, 0L, -1L}) {
            assertThrows(IllegalArgumentException.class, () -> contentService.isCompleted(invalid, f.student().getUserId()));
            assertThrows(IllegalArgumentException.class, () -> contentService.isCompleted(id, invalid));
        }
        assertThrows(java.util.NoSuchElementException.class,
            () -> contentService.isCompleted(Long.MAX_VALUE, f.student().getUserId()));
        var other = fixture();
        for (User denied : java.util.List.of(f.teacher(), other.student(), user())) {
            assertThrows(AccessDeniedException.class, () -> contentService.isCompleted(id, denied.getUserId()));
        }
        f.enrollment().deactivate(); em.flush();
        assertThrows(AccessDeniedException.class, () -> contentService.isCompleted(id, f.student().getUserId()));
    }

}
