package no.hvl.dat250.lms.content.repository;

import java.util.List;

import no.hvl.dat250.lms.content.domain.ChoiceOption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChoiceOptionRepository
        extends JpaRepository<ChoiceOption, Long> {

    List<ChoiceOption> findByQuestion_IdOrderByPositionAsc(
            Long questionId);
}