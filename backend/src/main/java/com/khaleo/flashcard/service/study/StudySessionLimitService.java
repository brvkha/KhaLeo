package com.khaleo.flashcard.service.study;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudySessionLimitService {

    private final StudyDailyQuotaService studyDailyQuotaService;

    public int remainingNewCardQuota(UUID userId) {
        return studyDailyQuotaService.remainingNewCardQuota(userId);
    }
}
