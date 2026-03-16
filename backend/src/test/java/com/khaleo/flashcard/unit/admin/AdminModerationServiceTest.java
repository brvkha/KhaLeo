package com.khaleo.flashcard.unit.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khaleo.flashcard.config.observability.NewRelicAuthInstrumentation;
import com.khaleo.flashcard.entity.AdminModerationAction;
import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.entity.enums.UserRole;
import com.khaleo.flashcard.repository.AdminModerationActionRepository;
import com.khaleo.flashcard.repository.UserRepository;
import com.khaleo.flashcard.service.admin.AdminModerationService;
import com.khaleo.flashcard.service.persistence.DeckCardAccessGuard;
import com.khaleo.flashcard.service.persistence.PersistenceValidationExceptionMapper;
import com.khaleo.flashcard.service.persistence.RelationalPersistenceService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminModerationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RelationalPersistenceService relationalPersistenceService;

    @Mock
    private AdminModerationActionRepository adminModerationActionRepository;

    @Mock
    private DeckCardAccessGuard deckCardAccessGuard;

    @Mock
    private NewRelicAuthInstrumentation newRelicAuthInstrumentation;

    private final PersistenceValidationExceptionMapper exceptionMapper = new PersistenceValidationExceptionMapper();

    private AdminModerationService adminModerationService;

    @BeforeEach
    void setUp() {
        adminModerationService = new AdminModerationService(
                userRepository,
                relationalPersistenceService,
                adminModerationActionRepository,
                deckCardAccessGuard,
                exceptionMapper,
                newRelicAuthInstrumentation);
    }

    @Test
    void shouldBanUserAndWriteAuditRecord() {
        UUID adminId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        User target = User.builder().id(targetId).email("u@example.com").passwordHash("h").role(UserRole.ROLE_USER).build();

        when(deckCardAccessGuard.requireAuthenticatedUserId("ban", "user", targetId.toString())).thenReturn(adminId);
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));

        adminModerationService.banUser(targetId);

        assertThat(target.getBannedAt()).isNotNull();
        assertThat(target.getBannedBy()).isEqualTo(adminId);

        verify(userRepository).save(target);

        ArgumentCaptor<AdminModerationAction> captor = ArgumentCaptor.forClass(AdminModerationAction.class);
        verify(adminModerationActionRepository).save(captor.capture());
        assertThat(captor.getValue().getAdminUserId()).isEqualTo(adminId);
        assertThat(captor.getValue().getTargetId()).isEqualTo(targetId);
        verify(newRelicAuthInstrumentation).recordAdminModerationOutcome(any(), any(), any());
    }
}
