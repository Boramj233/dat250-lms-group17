package no.hvl.dat250.lms.messaging;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import no.hvl.dat250.lms.content.domain.Assignment;
import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.enrollment.domain.CourseRole;
import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.submission.domain.Submission;
import no.hvl.dat250.lms.submission.domain.SubmissionAccepted;
import no.hvl.dat250.lms.submission.domain.SubmissionType;
import no.hvl.dat250.lms.submission.service.FileSubmissionService;
import no.hvl.dat250.lms.submission.service.SubmissionDeadlinePassedException;
import no.hvl.dat250.lms.submission.service.SubmissionService;
import no.hvl.dat250.lms.submission.storage.SubmissionObjectStorage;
import no.hvl.dat250.lms.user.domain.User;
import org.junit.jupiter.api.*;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/** Real RabbitMQ, JSON, listener, SQL commits and FILE coordinator; isolated H2 and mocked binary storage. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:messaging_it;DB_CLOSE_DELAY=-1",
    "spring.jpa.open-in-view=false", "lms.messaging.enabled=true"
})
@ActiveProfiles("test")
class SubmissionMessagingIntegrationTest {
    private static final String PASSWORD = UUID.randomUUID().toString();
    private static final GenericContainer<?> BROKER = new GenericContainer<>("rabbitmq:4.4.0-management")
        .withEnv("RABBITMQ_DEFAULT_USER", "test")
        .withEnv("RABBITMQ_DEFAULT_PASS", PASSWORD)
        .withExposedPorts(5672)
        .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(90)));

    @DynamicPropertySource
    static void rabbitProperties(DynamicPropertyRegistry registry) {
        BROKER.start();
        registry.add("spring.rabbitmq.host", BROKER::getHost);
        registry.add("spring.rabbitmq.port", () -> BROKER.getMappedPort(5672));
        registry.add("spring.rabbitmq.username", () -> "test");
        registry.add("spring.rabbitmq.password", () -> PASSWORD);
    }
    @AfterAll static void stopBroker() { BROKER.stop(); }

    @Autowired EntityManager entities;
    @Autowired PlatformTransactionManager transactions;
    @Autowired SubmissionService submissions;
    @Autowired FileSubmissionService files;
    @MockitoSpyBean RabbitTemplate rabbit;
    @MockitoBean SubmissionObjectStorage storage;
    @MockitoSpyBean SubmissionActivityListener listener;
    private RabbitAdmin admin;
    private String probe;

    @BeforeEach
    void probeQueue() {
        admin = new RabbitAdmin(rabbit.getConnectionFactory());
        probe = "test.submissions." + UUID.randomUUID();
        Queue queue = new Queue(probe, false, true, false);
        admin.declareQueue(queue);
        admin.declareBinding(BindingBuilder.bind(queue)
            .to(new TopicExchange(SubmissionMessagingConfig.EXCHANGE))
            .with(SubmissionMessagingConfig.ROUTING_KEY));
    }
    @AfterEach void removeProbe() { admin.deleteQueue(probe); }

    private <T> T save(T entity) { entities.persist(entity); entities.flush(); return entity; }
    private record Fixture(Long contentId, Long userId) {}
    private Fixture fixture(boolean closed) {
        return fixture(closed, true);
    }

    private Fixture fixture(boolean closed, boolean late) {
        return new TransactionTemplate(transactions).execute(status -> {
            User user = save(new User("message_" + UUID.randomUUID() + "@example.com", "Test", "Student"));
            Course course = save(new Course("DAT250", "Messaging", Term.AUTUMN, 2026));
            save(new Enrollment(user, course, CourseRole.STUDENT));
            Module module = save(new Module(course, "Week", 0));
            Assignment assignment = save(new Assignment(module, "Report", 0, "Write",
                late ? Instant.parse("2000-01-01T00:00:00Z") : Instant.parse("2099-01-01T00:00:00Z"), !closed));
            return new Fixture(assignment.getId(), user.getUserId());
        });
    }

    private SubmissionAccepted event(Long id, Long contentId) {
        return event(id, contentId, true);
    }

    private SubmissionAccepted event(Long id, Long contentId, boolean late) {
        Message message = rabbit.receive(probe, 5000);
        assertNotNull(message, "Committed submission must reach the broker");
        assertEquals(MessageProperties.CONTENT_TYPE_JSON, message.getMessageProperties().getContentType());
        assertEquals(MessageDeliveryMode.PERSISTENT, message.getMessageProperties().getReceivedDeliveryMode());
        var event = (SubmissionAccepted) rabbit.getMessageConverter().fromMessage(message);
        assertEquals(id, event.submissionId());
        assertEquals(contentId, event.contentId());
        assertNotNull(event.eventId());
        assertNotNull(event.submittedAt());
        assertEquals(late, event.late());
        String json = new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8);
        assertFalse(json.contains("payload"));
        assertFalse(json.contains("objectKey"));
        verify(listener, timeout(5000)).record(argThat(received -> received.eventId().equals(event.eventId())));
        return event;
    }

    @Test
    void textGitAndFileSubmissionsReachRealBrokerAndListenerAfterCommit() {
        var f = fixture(false);
        Submission text = submissions.submitIndividual(f.contentId(), f.userId(), SubmissionType.TEXT, "Private answer");
        var first = event(text.getId(), f.contentId());
        Submission git = submissions.submitIndividual(f.contentId(), f.userId(), SubmissionType.GIT_URL, "https://example.com/repo.git");
        var second = event(git.getId(), f.contentId());
        var file = files.submitFile(f.contentId(), f.userId(),
            new MockMultipartFile("file", "report.pdf", "application/pdf", "%PDF-1.7\nPrivate report".getBytes()));
        var third = event(file.id(), f.contentId());
        assertNotEquals(first.eventId(), second.eventId());
        assertNotEquals(second.eventId(), third.eventId());
        assertEquals(3, submissions.findMySubmissionHistory(f.contentId(), f.userId()).size());
        assertNull(rabbit.receive(probe, 200));
    }

    @Test
    void onTimeSubmissionPublishesWithoutLateFlag() {
        var f = fixture(false, false);
        Submission accepted = submissions.submitIndividual(
            f.contentId(), f.userId(), SubmissionType.TEXT, "On-time answer");
        assertFalse(accepted.isLate());
        event(accepted.getId(), f.contentId(), false);
    }

    @Test
    void brokerFailureAfterFileCommitDoesNotDeleteTheAcceptedFile() {
        var f = fixture(false);
        doThrow(new org.springframework.amqp.AmqpConnectException(
            new java.net.ConnectException("Injected broker outage after file commit")))
            .when(rabbit).convertAndSend(eq(SubmissionMessagingConfig.EXCHANGE),
                eq(SubmissionMessagingConfig.ROUTING_KEY), any(SubmissionAccepted.class));

        var accepted = assertDoesNotThrow(() -> files.submitFile(f.contentId(), f.userId(),
            new MockMultipartFile("file", "report.pdf", "application/pdf", "%PDF-1.7\nAccepted report".getBytes())));
        Submission stored = submissions.findOwnSubmission(accepted.id(), f.userId());
        assertEquals(SubmissionType.FILE, stored.getType());
        assertNotNull(stored.getFile());
        assertEquals("report.pdf", stored.getFile().getOriginalFilename());
        verify(storage).put(anyString(), any(java.io.InputStream.class), anyLong(), eq("application/pdf"));
        verify(storage, never()).delete(any());
        assertNull(rabbit.receive(probe, 300));
        verifyNoInteractions(listener);
    }

    @Test
    void eventWaitsForCommitAndRollbackDoesNotSend() {
        var f = fixture(false);
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            submissions.submitIndividual(f.contentId(), f.userId(), SubmissionType.TEXT, "Rolled back");
            assertNull(rabbit.receive(probe, 300));
            status.setRollbackOnly();
        });
        assertNull(rabbit.receive(probe, 500));
        assertTrue(submissions.findMySubmissionHistory(f.contentId(), f.userId()).isEmpty());
        verifyNoInteractions(listener);
    }

    @Test
    void brokerSendFailureKeepsTheAcceptedSubmissionCommitted() {
        var f = fixture(false);
        doThrow(new org.springframework.amqp.AmqpConnectException(
            new java.net.ConnectException("Injected broker outage")))
            .when(rabbit).convertAndSend(eq(SubmissionMessagingConfig.EXCHANGE),
                eq(SubmissionMessagingConfig.ROUTING_KEY), any(SubmissionAccepted.class));
        Submission accepted = assertDoesNotThrow(() -> submissions.submitIndividual(
            f.contentId(), f.userId(), SubmissionType.TEXT, "Accepted despite unavailable broker"));
        assertEquals(accepted.getId(), submissions.findOwnSubmission(accepted.getId(), f.userId()).getId());
        assertNull(rabbit.receive(probe, 300));
        verifyNoInteractions(listener);
    }

    @Test
    void rejectedDeadlineDoesNotPublish() {
        var f = fixture(true);
        assertThrows(SubmissionDeadlinePassedException.class,
            () -> submissions.submitIndividual(f.contentId(), f.userId(), SubmissionType.TEXT, "Rejected"));
        assertNull(rabbit.receive(probe, 500));
        verifyNoInteractions(listener);
    }
    @Test
    void groupSubmissionUsesExistingCommitEventAndRollbackPublishesNothing() {
        var f = fixture(false);
        Long groupId = new TransactionTemplate(transactions).execute(status -> {
            var content = entities.find(Assignment.class, f.contentId());
            var group = new no.hvl.dat250.lms.group.domain.Group(1, content.getModule().getCourse(), null);
            group.addGroupMember(entities.find(User.class, f.userId()));
            entities.persist(group); entities.flush();
            return group.getGroupId();
        });
        Long id = submissions.submit(f.contentId(), f.userId(), SubmissionType.TEXT, "Group", groupId).getId();
        event(id, f.contentId());
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            submissions.submit(f.contentId(), f.userId(), SubmissionType.GIT_URL, "https://example.com/group.git", groupId);
            status.setRollbackOnly();
        });
        assertNull(rabbit.receive(probe, 300));
    }

}
