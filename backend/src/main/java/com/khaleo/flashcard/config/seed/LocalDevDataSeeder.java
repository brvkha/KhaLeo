package com.khaleo.flashcard.config.seed;

import com.khaleo.flashcard.entity.Card;
import com.khaleo.flashcard.entity.Deck;
import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.entity.enums.UserRole;
import com.khaleo.flashcard.repository.CardRepository;
import com.khaleo.flashcard.repository.DeckRepository;
import com.khaleo.flashcard.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("!production")
@ConditionalOnProperty(prefix = "app.seed.local-dev", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class LocalDevDataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.local-dev.default-password:Passw0rd!}")
    private String defaultPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("local-dev seed started");

        Map<String, User> usersByEmail = seedUsers();
        seedDecksAndCards(usersByEmail);

        log.info("local-dev seed completed: users={}, decks={}, cards={}",
                userRepository.count(),
                deckRepository.count(),
                cardRepository.count());
    }

    private Map<String, User> seedUsers() {
        List<UserSeed> users = List.of(
            new UserSeed("admin@khaleo.app", UserRole.ROLE_ADMIN, true, false),
            new UserSeed("khaleo@khaleo.app", UserRole.ROLE_USER, true, false),
                new UserSeed("learner+01@khaleo.app", UserRole.ROLE_USER, true, false),
                new UserSeed("learner+02@khaleo.app", UserRole.ROLE_USER, true, false),
                new UserSeed("learner+03@khaleo.app", UserRole.ROLE_USER, true, false),
                new UserSeed("learner+04@khaleo.app", UserRole.ROLE_USER, true, false),
                new UserSeed("learner+05@khaleo.app", UserRole.ROLE_USER, true, false),
                new UserSeed("learner+blocked@khaleo.app", UserRole.ROLE_USER, true, true),
                new UserSeed("learner+unverified@khaleo.app", UserRole.ROLE_USER, false, false));

        List<User> savedUsers = new ArrayList<>();
        Instant now = Instant.now();
        for (UserSeed seed : users) {
            User user = userRepository.findByEmail(seed.email)
                    .orElseGet(User::new);
            user.setEmail(seed.email);
            user.setPasswordHash(passwordEncoder.encode(defaultPassword));
            user.setRole(seed.role);
            user.setIsEmailVerified(seed.verified);
            user.setDailyLearningLimit(9999);
            user.setFailedLoginAttempts(0);
            user.setAccountLockedUntil(null);
            user.setBannedAt(seed.banned ? now : null);
            user.setBannedBy(null);
            savedUsers.add(userRepository.save(user));
        }

        return savedUsers.stream().collect(java.util.stream.Collectors.toMap(User::getEmail, user -> user));
    }

    private void seedDecksAndCards(Map<String, User> usersByEmail) {
        List<DeckSeed> decks = List.of(
                new DeckSeed("PUB-EN-01", "learner+01@khaleo.app", true, "English", "Basic English phrases"),
                new DeckSeed("PUB-EN-02", "learner+02@khaleo.app", true, "English", "Intermediate English vocabulary"),
                new DeckSeed("PUB-EN-03", "learner+03@khaleo.app", true, "English", "Advanced English vocabulary"),
                new DeckSeed("PUB-MATH-01", "learner+04@khaleo.app", true, "Math", "Core algebra formulas"),
                new DeckSeed("PUB-SCI-01", "learner+05@khaleo.app", true, "Science", "Science terms and concepts"),
                new DeckSeed("PUB-HIST-01", "learner+01@khaleo.app", true, "History", "World history milestones"),
                new DeckSeed("PRI-L1-01", "learner+01@khaleo.app", false, "Private", "Learner 1 private deck"),
                new DeckSeed("PRI-L2-01", "learner+02@khaleo.app", false, "Private", "Learner 2 private deck"),
                new DeckSeed("PRI-L3-01", "learner+03@khaleo.app", false, "Private", "Learner 3 private deck"),
                new DeckSeed("PRI-L4-01", "learner+04@khaleo.app", false, "Private", "Learner 4 private deck"),
                new DeckSeed("PRI-L5-01", "learner+05@khaleo.app", false, "Private", "Learner 5 private deck"),
                new DeckSeed("PRI-ADMIN-01", "admin@khaleo.app", false, "Admin", "Admin private moderation deck"));

        for (DeckSeed deckSeed : decks) {
            User owner = usersByEmail.get(deckSeed.ownerEmail);
            if (owner == null) {
                throw new IllegalStateException("Missing seeded owner: " + deckSeed.ownerEmail);
            }

            Deck deck = findOrCreateDeck(owner, deckSeed);
            ensureDeckCards(deck, deckSeed);
        }
    }

    private Deck findOrCreateDeck(User owner, DeckSeed seed) {
        Deck existing = deckRepository.findByAuthorId(owner.getId()).stream()
                .filter(deck -> Objects.equals(deck.getName(), seed.code))
                .findFirst()
                .orElse(null);

        if (existing == null) {
            existing = new Deck();
            existing.setAuthor(owner);
            existing.setName(seed.code);
        }

        existing.setDescription(seed.description + " [seed]");
        existing.setTags(String.format(Locale.ROOT, "seed,%s,%s", seed.topic.toLowerCase(Locale.ROOT),
                seed.isPublic ? "public" : "private"));
        existing.setIsPublic(seed.isPublic);
        return deckRepository.save(existing);
    }

    private void ensureDeckCards(Deck deck, DeckSeed seed) {
        List<Card> existingCards = cardRepository.findByDeckId(deck.getId());
        int existingCount = existingCards.size();
        for (int i = existingCount + 1; i <= 10; i++) {
            String index = String.format(Locale.ROOT, "%02d", i);
            Card card = new Card();
            card.setDeck(deck);
            card.setFrontText(seed.code + "-Q-" + index);
            card.setBackText(seed.code + "-A-" + index);
            cardRepository.save(card);
        }
    }

    private record UserSeed(String email, UserRole role, boolean verified, boolean banned) {
    }

    private record DeckSeed(String code, String ownerEmail, boolean isPublic, String topic, String description) {
    }
}
