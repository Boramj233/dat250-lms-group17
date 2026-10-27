package no.hvl.dat250.lms.submission;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {
    // Mock repositories and construct SubmissionService when its API is defined.
    // Duplicate means the same student submitting for the same assignment,
    // regardless of submission ID or whether the submission is individual or group-based.

    @Test
    @Disabled("Not implemented yet")
    void submissionDeadlinePolicyIsEnforced() {
        // Use a fixed/injected clock: test before, exactly at, and after the deadline.
        // Define the exact boundary rule. Without a deadline, allow otherwise valid
        // submissions. After the deadline, reject if late submissions are disabled;
        // otherwise accept and mark the submission as late.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void onlyCourseTeacherCanGradeAndGiveFeedback() {
        // Allow the submission's course teacher to record a grade and written
        // feedback. Reject students and teachers assigned only to other courses.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void gradesRemainHiddenUntilTeacherPublishesThem() {
        // Keep grades/feedback hidden from students until release. After release,
        // students can read their own results, never another student's private results.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void groupGradeAppliesToEverySubmissionParticipant() {
        // Verify every participant in the group submission receives the same grade
        // and feedback under the same visibility policy.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void quizAttemptsUseTheirOwnAttemptLimit() {
        // Do not apply the one-submission-per-assignment rule to quiz attempts:
        // quizzes may explicitly allow multiple attempts.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void submissionMustBelongToExistingModule() {
        // Reject submissions without a module or referencing an unknown module.
        // Verify no submission is saved. The association may be through the assignment.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void studentMustBeEnrolledInModulesCourse() {
        // Resolve the course from the submission's module on the server. Accept
        // submission by a student enrolled in that course; reject a student with
        // no enrollment or enrollment only in a different course.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void everyGroupParticipantMustBeEnrolledInModulesCourse() {
        // Accept when all participating students are enrolled in the module's course.
        // Reject the whole group submission if even one participant is not enrolled;
        // verify no submission or participant records are saved.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void studentCanSubmitIndividually() {
        // Accept an individual submission when the student has not submitted that assignment.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void groupCanSubmitForItsMembers() {
        // Accept a group submission when none of its members has already submitted
        // that assignment. Count the submission for every participating member.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void studentCannotSubmitIndividuallyTwice() {
        // Reject a second individual submission for the same student and assignment.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void studentCannotSubmitIndividuallyAfterGroupSubmission() {
        // Reject an individual submission if the student participated in a previous
        // group submission for that assignment.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void groupCannotSubmitIfAnyMemberAlreadySubmittedIndividually() {
        // Reject the entire group submission if even one member already submitted
        // individually. Verify no submission or participant records are saved.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void groupCannotSubmitSameAssignmentTwice() {
        // Reject a repeat group submission with participants who already submitted.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void overlappingGroupsCannotSubmitSameAssignment() {
        // Reject a second group's submission if any member participated in another
        // group's submission for that assignment, even if other members are new.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void studentCanSubmitDifferentAssignments() {
        // Verify an existing submission does not block the student from submitting
        // a different assignment, individually or through a group.
        fail("Not implemented yet");
    }
}
