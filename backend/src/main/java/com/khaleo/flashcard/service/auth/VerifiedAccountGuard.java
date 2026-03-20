package com.khaleo.flashcard.service.auth;

import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.repository.UserRepository;
import com.khaleo.flashcard.service.persistence.PersistenceValidationExceptionMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VerifiedAccountGuard {

    private final UserRepository userRepository;
    private final PersistenceValidationExceptionMapper exceptionMapper;

    public void requireVerified(UUID userId, String operation, String resourceType, String resourceKey) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> exceptionMapper.userNotFound(userId));

        if (!Boolean.TRUE.equals(user.getIsEmailVerified())) {
            throw exceptionMapper.authorizationDenied(operation, resourceType, resourceKey);
        }
    }
}
