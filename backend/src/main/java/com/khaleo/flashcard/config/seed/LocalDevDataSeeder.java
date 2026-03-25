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
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.seed.local-dev.default-password:Passw0rd!}")
    private String defaultPassword;

    @Value("${app.seed.local-dev.reset-on-startup:true}")
    private boolean resetOnStartup;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("local-dev seed started");

        if (resetOnStartup) {
            resetAllDataTables();
        }

        Map<String, User> usersByEmail = seedUsers();
        seedDecksAndCards(usersByEmail);

        log.info("local-dev seed completed: users={}, decks={}, cards={}",
                userRepository.count(),
                deckRepository.count(),
                cardRepository.count());
    }

    private void resetAllDataTables() {
        String schema = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        if (schema == null || schema.isBlank()) {
            throw new IllegalStateException("Cannot resolve current database schema for local seed reset.");
        }

        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND table_type = 'BASE TABLE' AND table_name <> 'flyway_schema_history'",
                String.class,
                schema);

        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=0");
        try {
            for (String table : tables) {
                String safeTableName = table.replace("`", "``");
                jdbcTemplate.execute("TRUNCATE TABLE `" + safeTableName + "`");
            }
        } finally {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=1");
        }

        log.info("local-dev seed reset completed for schema={} tables={}", schema, tables.size());
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

        return savedUsers.stream().collect(Collectors.toMap(User::getEmail, user -> user));
    }

    private void seedDecksAndCards(Map<String, User> usersByEmail) {
        List<DeckSeed> decks = List.of(
                new DeckSeed("EN-VOC-CORE-1500", "learner+01@khaleo.app", true, "English",
                        "1500 practical English concepts for daily and professional communication"),
                new DeckSeed("EN-VOC-PHRASAL-VERBS", "learner+02@khaleo.app", true, "English",
                        "High-frequency phrasal verbs in context"),
                new DeckSeed("EN-GRAM-ESSENTIALS", "learner+03@khaleo.app", true, "English",
                        "Core grammar patterns with concise usage rules"),
                new DeckSeed("EN-SPEAKING-PATTERNS", "learner+04@khaleo.app", true, "English",
                        "Reusable sentence patterns for meetings, feedback, and daily conversation"),
                new DeckSeed("TECH-SPRING-BOOT", "learner+05@khaleo.app", true, "Engineering",
                        "Spring Boot architecture and operational concepts"),
                new DeckSeed("DATA-SQL-ESSENTIALS", "learner+02@khaleo.app", true, "Engineering",
                        "SQL querying, indexing, and transaction concepts"),
                new DeckSeed("PRODUCT-UX-FOUNDATIONS", "learner+03@khaleo.app", true, "Product",
                        "Core UX and product delivery concepts"),
                new DeckSeed("SCIENCE-BIO-CHEM", "learner+04@khaleo.app", true, "Science",
                        "Cross-topic biology and chemistry concepts"),
                new DeckSeed("HISTORY-WORLD-MILESTONES", "learner+05@khaleo.app", true, "History",
                        "Important milestones and causality in world history"),
                new DeckSeed("PRI-LEARNING-SCRATCH", "khaleo@khaleo.app", false, "Private",
                        "Private scratch deck for custom notes and experiments"),
                new DeckSeed("PRI-ADMIN-REVIEW", "admin@khaleo.app", false, "Admin",
                        "Admin moderation and quality review samples"));

        for (DeckSeed deckSeed : decks) {
            User owner = usersByEmail.get(deckSeed.ownerEmail);
            if (owner == null) {
                throw new IllegalStateException("Missing seeded owner: " + deckSeed.ownerEmail);
            }

            Deck deck = findOrCreateDeck(owner, deckSeed);
            seedCardsByDeckType(deck, deckSeed);
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

    private void seedCardsByDeckType(Deck deck, DeckSeed seed) {
        List<Card> existingCards = cardRepository.findByDeckId(deck.getId());
        if (!existingCards.isEmpty()) {
            return; // Already has cards
        }

        List<String[]> cardPairs = getCardContentByDeckCode(seed.code, getCardCountByDeckCode(seed.code));
        for (String[] pair : cardPairs) {
            Card card = new Card();
            card.setDeck(deck);
            card.setFrontText(pair[0]);
            card.setBackText(pair[1]);
            cardRepository.save(card);
        }
    }

    private int getCardCountByDeckCode(String deckCode) {
        return switch (deckCode) {
            case "EN-VOC-CORE-1500" -> 1500;
            case "EN-VOC-PHRASAL-VERBS" -> 180;
            case "EN-GRAM-ESSENTIALS" -> 120;
            case "EN-SPEAKING-PATTERNS" -> 120;
            case "TECH-SPRING-BOOT" -> 140;
            case "DATA-SQL-ESSENTIALS" -> 120;
            case "PRODUCT-UX-FOUNDATIONS" -> 100;
            case "SCIENCE-BIO-CHEM" -> 120;
            case "HISTORY-WORLD-MILESTONES" -> 120;
            case "PRI-LEARNING-SCRATCH" -> 60;
            case "PRI-ADMIN-REVIEW" -> 60;
            default -> 40;
        };
    }

    private List<String[]> getCardContentByDeckCode(String deckCode, int count) {
        return switch (deckCode) {
            case "EN-VOC-CORE-1500" -> getEnglishVocabularyCoreCards(count);
            case "EN-VOC-PHRASAL-VERBS" -> getPatternCards(
                    count,
                    List.of("look", "pick", "bring", "carry", "set", "take", "turn", "run", "break", "put"),
                    List.of("up", "down", "off", "on", "through", "out", "away", "over", "into", "back"),
                    "Phrasal Verb");
            case "EN-GRAM-ESSENTIALS" -> getPatternCards(
                    count,
                    List.of("present simple", "present continuous", "past simple", "past continuous", "future simple",
                            "present perfect", "past perfect", "modal verb", "passive voice", "conditional"),
                    List.of("affirmative", "negative", "question", "time marker", "common error", "formal use"),
                    "Grammar Pattern");
            case "EN-SPEAKING-PATTERNS" -> getPatternCards(
                    count,
                    List.of("meeting opener", "status update", "clarification request", "feedback statement", "follow-up request",
                            "disagreement phrase", "agreement phrase", "proposal phrase", "closing phrase", "small-talk starter"),
                    List.of("friendly", "neutral", "professional", "concise", "polite", "direct"),
                    "Speaking Pattern");
            case "TECH-SPRING-BOOT" -> getPatternCards(
                    count,
                    List.of("bean lifecycle", "dependency injection", "transaction boundary", "controller mapping", "repository pattern",
                            "service boundary", "security filter", "exception handler", "configuration property", "actuator endpoint"),
                    List.of("purpose", "pitfall", "best practice", "debugging tip", "production note", "testing note"),
                    "Spring Boot Concept");
            case "DATA-SQL-ESSENTIALS" -> getPatternCards(
                    count,
                    List.of("primary key", "foreign key", "covering index", "composite index", "join strategy",
                            "transaction isolation", "query plan", "normalization", "denormalization", "pagination"),
                    List.of("definition", "trade-off", "usage", "performance", "anti-pattern", "example"),
                    "SQL Concept");
            case "PRODUCT-UX-FOUNDATIONS" -> getPatternCards(
                    count,
                    List.of("user goal", "pain point", "value proposition", "information hierarchy", "feedback loop",
                            "error prevention", "empty state", "onboarding step", "success metric", "design constraint"),
                    List.of("discovery", "prototype", "delivery", "measurement", "iteration"),
                    "Product UX Concept");
            case "SCIENCE-BIO-CHEM" -> getPatternCards(
                    count,
                    List.of("cell membrane", "enzyme activity", "gene expression", "acid-base balance", "chemical bond",
                            "reaction rate", "energy transfer", "homeostasis", "molecular structure", "metabolic pathway"),
                    List.of("definition", "mechanism", "example", "application", "common misconception"),
                    "Science Concept");
            case "HISTORY-WORLD-MILESTONES" -> getPatternCards(
                    count,
                    List.of("agricultural revolution", "industrial revolution", "printing press", "global trade route", "nation state",
                            "civil rights movement", "cold war", "decolonization", "digital revolution", "public health reform"),
                    List.of("cause", "impact", "timeline", "stakeholders", "legacy"),
                    "History Concept");
            default -> getGenericCards(deckCode, count);
        };
    }

    private List<String[]> getEnglishVocabularyCoreCards(int count) {
        List<String[]> cards = new ArrayList<>(count);

        List<String> modifiers = List.of(
                "adaptive", "accurate", "active", "agile", "aligned", "analytical", "assertive", "authentic", "balanced", "bold",
                "calm", "capable", "clear", "collaborative", "confident", "consistent", "creative", "critical", "curious", "decisive",
                "deliberate", "detailed", "diligent", "dynamic", "efficient", "empathetic", "focused", "flexible", "forward", "global",
                "honest", "inclusive", "independent", "innovative", "insightful", "intentional", "logical", "methodical", "practical", "precise",
                "proactive", "reflective", "reliable", "resilient", "strategic", "structured", "systematic", "thoughtful", "timely", "versatile");

        List<String> domains = List.of(
                "communication", "learning", "planning", "execution", "feedback", "collaboration", "analysis", "writing", "negotiation", "leadership",
                "presentation", "problem solving", "time management", "decision making", "research", "documentation", "customer support", "quality control", "risk management", "innovation",
                "coaching", "mentoring", "prioritization", "stakeholder management", "project delivery", "team alignment", "conflict resolution", "continuous improvement", "career growth", "professional networking");

        for (String modifier : modifiers) {
            for (String domain : domains) {
                if (cards.size() >= count) {
                    return cards;
                }
                String concept = modifier + " " + domain;
                String explanation = String.format(Locale.ROOT,
                        "A concept describing how to apply %s behavior to %s in practical situations.",
                        modifier,
                        domain);
                cards.add(new String[] {concept, explanation});
            }
        }

        while (cards.size() < count) {
            int idx = cards.size() + 1;
            cards.add(new String[] {
                    String.format(Locale.ROOT, "vocabulary concept %04d", idx),
                    String.format(Locale.ROOT, "A concise definition for vocabulary concept %04d.", idx)
            });
        }

        return cards;
    }

    private List<String[]> getPatternCards(int count, List<String> topics, List<String> dimensions, String label) {
        List<String[]> cards = new ArrayList<>(count);
        int serial = 1;

        while (cards.size() < count) {
            for (String topic : topics) {
                for (String dimension : dimensions) {
                    if (cards.size() >= count) {
                        return cards;
                    }
                    String front = topic + " - " + dimension;
                    String back = String.format(Locale.ROOT,
                            "%s %03d: explain %s with focus on %s, including one practical example.",
                            label,
                            serial,
                            topic,
                            dimension);
                    cards.add(new String[] {front, back});
                    serial++;
                }
            }
        }

        return cards;
    }

    private List<String[]> getGenericCards(String deckCode, int count) {
        List<String[]> cards = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            String index = String.format(Locale.ROOT, "%02d", i);
            cards.add(new String[]{
                    deckCode + " concept " + index,
                    "Explain the concept shown on the front in one concise paragraph."
            });
        }
        return cards;
    }

    private record UserSeed(String email, UserRole role, boolean verified, boolean banned) {
    }

    private record DeckSeed(String code, String ownerEmail, boolean isPublic, String topic, String description) {
    }
}
