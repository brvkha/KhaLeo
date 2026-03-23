package com.khaleo.flashcard.unit.study;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.khaleo.flashcard.config.observability.NewRelicDeckMediaInstrumentation;
import com.khaleo.flashcard.model.dynamo.RatingGiven;
import com.khaleo.flashcard.model.dynamo.StudyActivityLog;
import com.khaleo.flashcard.repository.dynamo.StudyActivityLogRepository;
import com.khaleo.flashcard.service.activitylog.ActivityLogRetryService;
import com.khaleo.flashcard.service.activitylog.StudyActivityLogPublisher;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class StudyActivityLogPublisherTest {

    private StudyActivityLogRepository repository;
    private ActivityLogRetryService retryService;
    private StudyActivityLogPublisher publisher;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(StudyActivityLogRepository.class);
        retryService = new ActivityLogRetryService();
        publisher = new StudyActivityLogPublisher(repository, retryService, Mockito.mock(NewRelicDeckMediaInstrumentation.class));
    }

    @Test
    void shouldPublishEnrichedRatingPayload() {
        publisher.publishRatingEvent(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        RatingGiven.EASY,
                        1200L,
                        4,
                BigDecimal.valueOf(2.65),
                BigDecimal.valueOf(4.8))
                .join();

        verify(repository, times(1)).save(any(StudyActivityLog.class));
    }

    @Test
    void shouldNotThrowWhenPublishFails() {
        Mockito.doThrow(new RuntimeException("down")).when(repository).save(any(StudyActivityLog.class));

        publisher.publishRatingEvent(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        RatingGiven.GOOD,
                        500L,
                        1,
                BigDecimal.valueOf(2.5),
                BigDecimal.valueOf(5.1))
                .join();

        assertThat(retryService.deadLetterCount()).isEqualTo(1);
    }
}
