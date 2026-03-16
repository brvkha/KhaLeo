package com.khaleo.flashcard.service.study;

import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.entity.enums.CardLearningStateType;
import com.khaleo.flashcard.repository.CardLearningStateRepository;
import com.khaleo.flashcard.repository.UserRepository;
import com.khaleo.flashcard.service.persistence.PersistenceValidationExceptionMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudyDailyQuotaService {

    private final UserRepository userRepository;
    private final CardLearningStateRepository cardLearningStateRepository;
    private final PersistenceValidationExceptionMapper exceptionMapper;

    public int remainingNewCardQuota(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> exceptionMapper.missingRelationship("user", userId.toString()));

        ZoneId zone = ZoneId.systemDefault();
        Instant startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant();
        Instant startOfNextDay = startOfDay.plusSeconds(24L * 60L * 60L);

        long studiedToday = cardLearningStateRepository
                .countByUserIdAndStateNotAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        userId,
                        CardLearningStateType.NEW,
                        startOfDay,
                        startOfNextDay);

        return Math.max(user.getDailyLearningLimit() - (int) studiedToday, 0);
    }
}
