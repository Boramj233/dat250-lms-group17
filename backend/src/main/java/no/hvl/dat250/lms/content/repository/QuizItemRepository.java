package no.hvl.dat250.lms.content.repository;

import java.util.UUID;
import no.hvl.dat250.lms.content.domain.QuizItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizItemRepository extends JpaRepository<QuizItem, UUID> {
}
