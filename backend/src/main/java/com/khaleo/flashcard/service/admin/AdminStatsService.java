package com.khaleo.flashcard.service.admin;

import com.khaleo.flashcard.repository.CardLearningStateRepository;
import com.khaleo.flashcard.repository.CardRepository;
import com.khaleo.flashcard.repository.DeckRepository;
import com.khaleo.flashcard.repository.UserRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatsService {

    private final UserRepository userRepository;
    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final CardLearningStateRepository cardLearningStateRepository;

    public StatsSnapshot getPlatformStats() {
        Instant now = Instant.now();
        Instant since = now.minusSeconds(24 * 60 * 60);
        return new StatsSnapshot(
                userRepository.count(),
                deckRepository.count(),
                cardRepository.count(),
                cardLearningStateRepository.countByLastReviewedAtGreaterThanEqual(since),
                now);
    }

    public record StatsSnapshot(
            long totalUsers,
            long totalDecks,
            long totalCards,
            long reviewsLast24Hours,
            Instant generatedAt) {
    }
}
