package no.hvl.dat250.lms.submission;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SubmissionIntegrationTest {
    @Test
    @Disabled("Not implemented yet")
    void deadlinePolicyAndLateFlagWorkThroughApi() {
        // With a controlled clock, verify rejection or acceptance of late submissions
        // according to the teacher's setting, and that the late flag survives reload.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void gradingAndReleaseAreEnforcedThroughApi() {
        // Course teacher grades and adds feedback; persist/reload and verify the
        // student cannot see results before release but can see their own afterward.
        // Reject grading by students/other-course teachers and reading others' results.
        // Verify unpublished results are absent from list and detail responses too.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void groupMembersRetrieveSameReleasedGrade() {
        // Grade a group submission, publish it, then verify each participating
        // student's API response contains the same grade and feedback.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void submissionModuleCourseAssociationCanBeRetrieved() {
        // Persist a course, its module, and a submission (through its assignment if
        // applicable). Flush/clear and verify the submission resolves to that module
        // and course. Keep duplicate prevention scoped per assignment, not per module.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void databaseRejectsSubmissionWithoutValidModuleAssociation() {
        // Verify missing and nonexistent module associations are rejected on flush.
        // If linked through an assignment, enforce the required foreign keys along
        // that chain. Use a separate test for each constraint failure.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void enrollmentIsRequiredForSubmission() {
        // Through the real service/API, accept a submission by an enrolled student.
        // In separate tests, reject no enrollment and enrollment in another course.
        // Verify rejected requests leave no submission or participant records.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void groupSubmissionRequiresEnrollmentForEveryParticipant() {
        // Accept a group whose participating students are all enrolled in the module's
        // course. Reject a group with one unenrolled participant without partial data.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void individualAndGroupSubmissionsCanBePersisted() {
        // In separate scenarios, persist an individual submission and a group submission.
        // Flush/clear, reload, and verify the assignment and participating students.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void duplicateParticipationIsRejectedAcrossSubmissionTypes() {
        // Verify persistence prevents two submissions for the same student and
        // assignment: individual/individual, individual/group, group/individual,
        // and group/group (including different groups with overlapping members).
        // Choose a persistence model that enforces uniqueness across both types,
        // e.g. unique (student_id, assignment_id) in submission participant records.
        // Uniqueness on submission ID or group ID alone is insufficient.
        // Flush to trigger constraints and use a separate test for each failure.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void rejectedGroupSubmissionLeavesNoPartialData() {
        // With one member already submitted, attempt a group submission through
        // the service/API transaction. Verify no partial submission or participant
        // records remain, and the existing submission is unchanged.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void submissionsForDifferentAssignmentsRemainAllowed() {
        // Persist submissions for the same student on different assignments and
        // verify they can both be retrieved.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void submissionApiRejectsDuplicateSubmissions() {
        // When endpoints exist, use MockMvc to verify duplicate submissions
        // return the agreed HTTP error for both individual and group requests.
        fail("Not implemented yet");
    }
}
