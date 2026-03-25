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

        return savedUsers.stream().collect(java.util.stream.Collectors.toMap(User::getEmail, user -> user));
    }

    private void seedDecksAndCards(Map<String, User> usersByEmail) {
        List<DeckSeed> decks = List.of(
                // English Vocabulary - Beginner Level
                new DeckSeed("EN-VOC-A1-BASICS", "learner+01@khaleo.app", true, "English", 
                        "A1 Level: Basic Words & Phrases"),
                new DeckSeed("EN-VOC-A1-DAILY", "learner+02@khaleo.app", true, "English", 
                        "A1 Level: Daily Conversations"),
                new DeckSeed("EN-VOC-A1-FOOD", "learner+03@khaleo.app", true, "English", 
                        "A1 Level: Food & Drinks"),
                new DeckSeed("EN-VOC-A1-ANIMALS", "learner+04@khaleo.app", true, "English", 
                        "A1 Level: Animals & Nature"),
                
                // English Vocabulary - Intermediate Level
                new DeckSeed("EN-VOC-B1-WORK", "learner+05@khaleo.app", true, "English", 
                        "B1 Level: Business & Work"),
                new DeckSeed("EN-VOC-B1-TRAVEL", "learner+01@khaleo.app", true, "English", 
                        "B1 Level: Travel & Tourism"),
                new DeckSeed("EN-VOC-B1-HEALTH", "learner+02@khaleo.app", true, "English", 
                        "B1 Level: Health & Medicine"),
                new DeckSeed("EN-VOC-B1-EMOTIONS", "learner+03@khaleo.app", true, "English", 
                        "B1 Level: Emotions & Feelings"),
                
                // English Vocabulary - Advanced Level
                new DeckSeed("EN-VOC-B2-ACADEMIC", "learner+04@khaleo.app", true, "English", 
                        "B2 Level: Academic English"),
                new DeckSeed("EN-VOC-B2-PHRASAL", "learner+05@khaleo.app", true, "English", 
                        "B2 Level: Phrasal Verbs"),
                new DeckSeed("EN-VOC-B2-IDIOMS", "learner+01@khaleo.app", true, "English", 
                        "B2 Level: Common Idioms"),
                new DeckSeed("EN-VOC-B2-BUSINESS", "learner+02@khaleo.app", true, "English", 
                        "B2 Level: Business Terminology"),
                
                // English Grammar Topics
                new DeckSeed("EN-GRAM-TENSES", "learner+03@khaleo.app", true, "English", 
                        "Grammar: English Tenses"),
                new DeckSeed("EN-GRAM-PREPOSITIONS", "learner+04@khaleo.app", true, "English", 
                        "Grammar: Prepositions"),
                new DeckSeed("EN-GRAM-CONDITIONALS", "learner+05@khaleo.app", true, "English", 
                        "Grammar: Conditional Sentences"),
                new DeckSeed("EN-GRAM-PASSIVE", "learner+01@khaleo.app", true, "English", 
                        "Grammar: Passive Voice"),
                
                // Other Subjects
                new DeckSeed("MATH-ALGEBRA", "learner+02@khaleo.app", true, "Math", 
                        "Algebra: Equations & Functions"),
                new DeckSeed("MATH-GEOMETRY", "learner+03@khaleo.app", true, "Math", 
                        "Geometry: Shapes & Formulas"),
                new DeckSeed("MATH-CALCULUS", "learner+04@khaleo.app", true, "Math", 
                        "Calculus: Derivatives & Integrals"),
                new DeckSeed("SCI-BIOLOGY", "learner+05@khaleo.app", true, "Science", 
                        "Biology: Cells & Organisms"),
                new DeckSeed("SCI-CHEMISTRY", "learner+01@khaleo.app", true, "Science", 
                        "Chemistry: Elements & Reactions"),
                new DeckSeed("HIST-ANCIENT", "learner+02@khaleo.app", true, "History", 
                        "Ancient Civilizations"),
                new DeckSeed("HIST-MEDIEVAL", "learner+03@khaleo.app", true, "History", 
                        "Medieval Period"),
                new DeckSeed("HIST-MODERN", "learner+04@khaleo.app", true, "History", 
                        "Modern History"),
                
                // Private Learning Decks
                new DeckSeed("PRI-L1-PRACTICE", "learner+01@khaleo.app", false, "Private", 
                        "Personal Practice - Level 1"),
                new DeckSeed("PRI-L2-PRACTICE", "learner+02@khaleo.app", false, "Private", 
                        "Personal Practice - Level 2"),
                new DeckSeed("PRI-L3-PRACTICE", "learner+03@khaleo.app", false, "Private", 
                        "Personal Practice - Level 3"),
                new DeckSeed("PRI-L4-PRACTICE", "learner+04@khaleo.app", false, "Private", 
                        "Personal Practice - Level 4"),
                new DeckSeed("PRI-L5-PRACTICE", "learner+05@khaleo.app", false, "Private", 
                        "Personal Practice - Level 5"),
                new DeckSeed("PRI-ADMIN-REVIEW", "admin@khaleo.app", false, "Admin", 
                        "Admin Content Review Deck"));

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

    private void seedCardsByDeckType(Deck deck, DeckSeed seed) {
        List<Card> existingCards = cardRepository.findByDeckId(deck.getId());
        if (!existingCards.isEmpty()) {
            return; // Already has cards
        }

        List<String[]> cardPairs = getCardContentByDeckCode(seed.code);
        for (String[] pair : cardPairs) {
            Card card = new Card();
            card.setDeck(deck);
            card.setFrontText(pair[0]);
            card.setBackText(pair[1]);
            cardRepository.save(card);
        }
    }

    private List<String[]> getCardContentByDeckCode(String deckCode) {
        return switch (deckCode) {
            case "EN-VOC-A1-BASICS" -> List.of(
                    new String[]{"Hello", "Xin chào"},
                    new String[]{"Thank you", "Cảm ơn"},
                    new String[]{"Please", "Vui lòng"},
                    new String[]{"Yes", "Có"},
                    new String[]{"No", "Không"},
                    new String[]{"Good morning", "Chào buổi sáng"},
                    new String[]{"Good night", "Chào buổi tối"},
                    new String[]{"How are you?", "Bạn khỏe không?"},
                    new String[]{"My name is...", "Tên tôi là..."},
                    new String[]{"What is your name?", "Tên bạn là gì?"}
            );
            case "EN-VOC-A1-DAILY" -> List.of(
                    new String[]{"I am happy", "Tôi rất vui"},
                    new String[]{"I am tired", "Tôi rất mệt"},
                    new String[]{"I am hungry", "Tôi đói"},
                    new String[]{"I am thirsty", "Tôi khát"},
                    new String[]{"Do you speak English?", "Bạn có nói tiếng Anh không?"},
                    new String[]{"I don't understand", "Tôi không hiểu"},
                    new String[]{"Can you help me?", "Bạn có thể giúp tôi không?"},
                    new String[]{"Where is the bathroom?", "Nhà vệ sinh ở đâu?"},
                    new String[]{"How much does it cost?", "Nó giá bao nhiêu?"},
                    new String[]{"What time is it?", "Bây giờ mấy giờ?"}
            );
            case "EN-VOC-A1-FOOD" -> List.of(
                    new String[]{"Apple", "Táo"},
                    new String[]{"Banana", "Chuối"},
                    new String[]{"Bread", "Bánh mì"},
                    new String[]{"Cheese", "Phô mai"},
                    new String[]{"Chicken", "Gà"},
                    new String[]{"Rice", "Cơm"},
                    new String[]{"Water", "Nước"},
                    new String[]{"Coffee", "Cà phê"},
                    new String[]{"Tea", "Trà"},
                    new String[]{"Milk", "Sữa"}
            );
            case "EN-VOC-A1-ANIMALS" -> List.of(
                    new String[]{"Dog", "Chó"},
                    new String[]{"Cat", "Mèo"},
                    new String[]{"Bird", "Chim"},
                    new String[]{"Fish", "Cá"},
                    new String[]{"Lion", "Sư tử"},
                    new String[]{"Elephant", "Voi"},
                    new String[]{"Tiger", "Hổ"},
                    new String[]{"Bear", "Gấu"},
                    new String[]{"Tree", "Cây"},
                    new String[]{"Flower", "Hoa"}
            );
            case "EN-VOC-B1-WORK" -> List.of(
                    new String[]{"Conference | A formal meeting", "Hội thảo | Một cuộc họp chính thức"},
                    new String[]{"Deadline | The final date/time", "Hạn chót | Ngày/giờ cuối cùng"},
                    new String[]{"Project | A planned piece of work", "Dự án | Một công việc được lên kế hoạch"},
                    new String[]{"Team | A group working together", "Đội | Một nhóm làm việc cùng nhau"},
                    new String[]{"Manager | Someone in charge", "Quản lý | Người chịu trách nhiệm"},
                    new String[]{"Report | A detailed account", "Báo cáo | Một tài khoản chi tiết"},
                    new String[]{"Budget | Planned spending", "Ngân sách | Chi tiêu được lên kế hoạch"},
                    new String[]{"Client | A customer/buyer", "Khách hàng | Một người mua"},
                    new String[]{"Colleague | A co-worker", "Đồng nghiệp | Một người cùng làm việc"},
                    new String[]{"Promotion | Advancement in job", "Thăng chức | Tiến bộ trong công việc"}
            );
            case "EN-VOC-B1-TRAVEL" -> List.of(
                    new String[]{"Passport | Travel document", "Hộ chiếu | Tài liệu du lịch"},
                    new String[]{"Visa | Permission to enter", "Thị thực | Giấy phép vào cảng"},
                    new String[]{"Airport | Place for flights", "Sân bay | Nơi có máy bay"},
                    new String[]{"Hotel | Place to stay", "Khách sạn | Nơi ở lại"},
                    new String[]{"Luggage | Bags for travel", "Hành lý | Túi du lịch"},
                    new String[]{"Booking | Reserving ahead", "Đặt phòng | Dự trữ trước"},
                    new String[]{"Tour | Guided trip", "Tour | Chuyến tham quan có hướng dẫn"},
                    new String[]{"Destination | End location", "Điểm đến | Địa điểm cuối cùng"},
                    new String[]{"Currency | Money of a country", "Tiền tệ | Tiền của một đất nước"},
                    new String[]{"Souvenir | Memorabilia from trip", "Quà lưu niệm | Vật lưu niệm từ chuyến đi"}
            );
            case "EN-VOC-B1-HEALTH" -> List.of(
                    new String[]{"Doctor | Medical professional", "Bác sĩ | Chuyên gia y tế"},
                    new String[]{"Medicine | Drug for treatment", "Thuốc | Chất trị liệu"},
                    new String[]{"Hospital | Medical facility", "Bệnh viện | Cơ sở y tế"},
                    new String[]{"Symptom | Sign of illness", "Triệu chứng | Dấu hiệu bệnh"},
                    new String[]{"Prescription | Doctor's order", "Đơn thuốc | Lệnh của bác sĩ"},
                    new String[]{"Exercise | Physical activity", "Tập thể dục | Hoạt động thể chất"},
                    new String[]{"Diet | Daily food intake", "Chế độ ăn | Lượng thức ăn hàng ngày"},
                    new String[]{"Fitness | State of health", "Sức khỏe | Tình trạng sức khỏe"},
                    new String[]{"Vaccine | Preventive medicine", "Vắc-xin | Thuốc phòng ngừa"},
                    new String[]{"Allergy | Adverse reaction", "Dị ứng | Phản ứng bất lợi"}
            );
            case "EN-VOC-B1-EMOTIONS" -> List.of(
                    new String[]{"Angry | Feeling rage", "Tức giận | Cảm thấy giận dữ"},
                    new String[]{"Sad | Feeling sorrow", "Buồn | Cảm thấy u sầu"},
                    new String[]{"Happy | Feeling joy", "Vui | Cảm thấy vui vẻ"},
                    new String[]{"Anxious | Feeling worried", "Lo lắng | Cảm thấy lo âu"},
                    new String[]{"Calm | Peaceful state", "Bình tĩnh | Trạng thái yên bình"},
                    new String[]{"Excited | Full of enthusiasm", "H興奮 | Đầy nhiệt tình"},
                    new String[]{"Nervous | Feeling tension", "Căng thẳng | Cảm thấy căng thẳng"},
                    new String[]{"Proud | Feeling satisfied", "Tự hào | Cảm thấy hài lòng"},
                    new String[]{"Disappointed | Let down", "Thất vọng | Bị tổn thương"},
                    new String[]{"Confused | Not clear", "Bối rối | Không rõ ràng"}
            );
            case "EN-VOC-B2-ACADEMIC" -> List.of(
                    new String[]{"Thesis | Long academic paper", "Luận án | Bài báo học thuật dài"},
                    new String[]{"Hypothesis | Proposed idea", "Giả thuyết | Ý tưởng được đề xuất"},
                    new String[]{"Analysis | Detailed examination", "Phân tích | Kiểm tra chi tiết"},
                    new String[]{"Conclusion | Final judgment", "Kết luận | Quyết định cuối cùng"},
                    new String[]{"Evidence | Supporting facts", "Bằng chứng | Những sự kiện hỗ trợ"},
                    new String[]{"Source | Origin of information", "Nguồn | Gốc của thông tin"},
                    new String[]{"Citation | Reference to source", "Trích dẫn | Tham chiếu đến nguồn"},
                    new String[]{"Peer review | Expert evaluation", "Đánh giá ngang hàng | Đánh giá chuyên gia"},
                    new String[]{"Methodology | Research method", "Phương pháp | Phương pháp nghiên cứu"},
                    new String[]{"Abstract | Paper summary", "Tóm tắt | Tóm tắt bài báo"}
            );
            case "EN-VOC-B2-PHRASAL" -> List.of(
                    new String[]{"Look after | Take care of", "Chăm sóc | Trông coi"},
                    new String[]{"Put off | Postpone", "Hoãn lại | Trì hoãn"},
                    new String[]{"Bring up | Raise/mention", "Nêu lên | Đề cập"},
                    new String[]{"Run out | Exhaust supply", "Hết | Cạn kiệt"},
                    new String[]{"Come across | Find by chance", "Tình cờ gặp | Tìm thấy tình cờ"},
                    new String[]{"Stand out | Be noticeable", "Nổi bật | Dễ nhận thấy"},
                    new String[]{"Give up | Quit/surrender", "Bỏ cuộc | Từ bỏ"},
                    new String[]{"Take on | Accept/employ", "Nhận lấy | Sở hữu"},
                    new String[]{"Back up | Support/reverse", "Hỗ trợ | Sao lưu"},
                    new String[]{"Break down | Fail/analyze", "Hỏng/phân tích | Sự cố"}
            );
            case "EN-VOC-B2-IDIOMS" -> List.of(
                    new String[]{"Break the ice | Start conversation", "Phá tan sự lạnh nhạt | Bắt đầu cuộc trò chuyện"},
                    new String[]{"Piece of cake | Very easy", "Dễ dàng như ăn bánh | Rất dễ dàng"},
                    new String[]{"Rain cats and dogs | Rain heavily", "Mưa nặng hạt | Mưa rất to"},
                    new String[]{"Hit the books | Study hard", "Ôn bài | Học tập chăm chỉ"},
                    new String[]{"Spill the beans | Tell secret", "Tiết lộ bí mật | Nói ra bí mật"},
                    new String[]{"Bite the dust | Fail/die", "Thất bại | Chết"},
                    new String[]{"Down to earth | Practical", "Thực tế | Cách tiếp cận thực tế"},
                    new String[]{"Every cloud has silver lining | Hope", "Mọi mây đều có lớp bạc | Hy vọng"},
                    new String[]{"Get the ball rolling | Start action", "Bắt đầu | Khởi động"},
                    new String[]{"Keep your fingers crossed | Hope for luck", "Mong điều tốt | Hy vọng may mắn"}
            );
            case "EN-VOC-B2-BUSINESS" -> List.of(
                    new String[]{"Merger | Joining of companies", "Sáp nhập | Kết hợp công ty"},
                    new String[]{"Investment | Money put in", "Đầu tư | Tiền đầu tư"},
                    new String[]{"Profit | Money gained", "Lợi nhuận | Tiền kiếm được"},
                    new String[]{"Dividend | Share of profit", "Cổ tức | Chia sẻ lợi nhuận"},
                    new String[]{"Asset | Something valuable", "Tài sản | Cái gì có giá trị"},
                    new String[]{"Liability | Debt/obligation", "Nợ | Trách nhiệm"},
                    new String[]{"Revenue | Income from sales", "Doanh thu | Thu nhập từ bán hàng"},
                    new String[]{"Expense | Cost of operating", "Chi phí | Chi phí hoạt động"},
                    new String[]{"Cash flow | Money movement", "Dòng tiền | Chuyển động tiền"},
                    new String[]{"Stakeholder | Interested party", "Bên liên quan | Người có quyền lợi"}
            );
            case "EN-GRAM-TENSES" -> List.of(
                    new String[]{"Simple Present | I work", "Hiện tại đơn | Tôi làm việc"},
                    new String[]{"Present Continuous | I am working", "Hiện tại tiếp diễn | Tôi đang làm việc"},
                    new String[]{"Present Perfect | I have worked", "Hiện tại hoàn thành | Tôi đã làm việc"},
                    new String[]{"Simple Past | I worked", "Quá khứ đơn | Tôi đã làm việc"},
                    new String[]{"Past Continuous | I was working", "Quá khứ tiếp diễn | Tôi đang làm việc"},
                    new String[]{"Past Perfect | I had worked", "Quá khứ hoàn thành | Tôi đã làm việc"},
                    new String[]{"Simple Future | I will work", "Tương lai đơn | Tôi sẽ làm việc"},
                    new String[]{"Future Continuous | I will be working", "Tương lai tiếp diễn | Tôi sẽ đang làm việc"},
                    new String[]{"Future Perfect | I will have worked", "Tương lai hoàn thành | Tôi sẽ đã làm việc"},
                    new String[]{"Conditional | I would work", "Điều kiện | Tôi sẽ làm việc"}
            );
            case "EN-GRAM-PREPOSITIONS" -> List.of(
                    new String[]{"at | Specific time/place", "ở | Thời gian/nơi cụ thể"},
                    new String[]{"in | Inside/during", "trong | Bên trong/trong suốt"},
                    new String[]{"on | Surface/day", "trên | Bề mặt/ngày"},
                    new String[]{"by | Beside/agent", "bởi | Bên cạnh/nhân vật"},
                    new String[]{"for | Purpose/duration", "cho | Mục đích/thời lượng"},
                    new String[]{"from | Origin", "từ | Nguồn gốc"},
                    new String[]{"to | Destination", "đến | Điểm đến"},
                    new String[]{"with | Accompanied by", "với | Đi kèm với"},
                    new String[]{"about | Concerning", "về | Liên quan đến"},
                    new String[]{"between | In middle of two", "giữa | Ở giữa hai cái"}
            );
            case "EN-GRAM-CONDITIONALS" -> List.of(
                    new String[]{"Zero Conditional | If + present, present", "Điều kiện không | Nếu + hiện tại, hiện tại"},
                    new String[]{"First Conditional | If + present, will + verb", "Điều kiện thứ nhất | Nếu + hiện tại, sẽ + động từ"},
                    new String[]{"Second Conditional | If + past, would + verb", "Điều kiện thứ hai | Nếu + quá khứ, sẽ + động từ"},
                    new String[]{"Third Conditional | If + had + past participle, would have + verb", "Điều kiện thứ ba | Nếu + đã + quá khứ phân từ, sẽ đã"},
                    new String[]{"Mixed Conditional | If + past, would + present", "Hỗn hợp điều kiện | Nếu + quá khứ, sẽ + hiện tại"},
                    new String[]{"unless | If not", "trừ khi | Nếu không"},
                    new String[]{"provided that | On condition that", "với điều kiện | Trên điều kiện"},
                    new String[]{"as long as | Provided that", "miễn là | Với điều kiện"},
                    new String[]{"in case | If it happens", "trong trường hợp | Nếu xảy ra"},
                    new String[]{"even if | Regardless of", "ngay cả nếu | Bất kể"}
            );
            case "EN-GRAM-PASSIVE" -> List.of(
                    new String[]{"Present Passive | is + past participle", "Bị động hiện tại | is + quá khứ phân từ"},
                    new String[]{"Past Passive | was + past participle", "Bị động quá khứ | was + quá khứ phân từ"},
                    new String[]{"Future Passive | will be + past participle", "Bị động tương lai | will be + quá khứ phân từ"},
                    new String[]{"Perfect Passive | has/have + been + past participle", "Bị động hoàn thành | has/have + been"},
                    new String[]{"Passive Modal | must/can/should + be + past participle", "Bị động khiếp | must/can + be"},
                    new String[]{"Gerund Passive | being + past participle", "Gerund bị động | being + quá khứ phân từ"},
                    new String[]{"Infinitive Passive | to be + past participle", "Bất định bị động | to be + quá khứ phân từ"},
                    new String[]{"Active to Passive | Conversion pattern", "Chủ động sang bị động | Mẫu chuyển đổi"},
                    new String[]{"Agent in Passive | by + agent", "Tác nhân trong bị động | by + tác nhân"},
                    new String[]{"Passive Focus | Emphasizes action/object", "Tiêu điểm bị động | Nhấn mạnh hành động"}
            );
            case "MATH-ALGEBRA" -> List.of(
                    new String[]{"Linear equation | ax + b = 0", "Phương trình bậc nhất | ax + b = 0"},
                    new String[]{"Quadratic equation | ax² + bx + c = 0", "Phương trình bậc hai | ax² + bx + c = 0"},
                    new String[]{"Variable | Unknown value", "Biến số | Giá trị không xác định"},
                    new String[]{"Coefficient | Number before variable", "Hệ số | Số trước biến"},
                    new String[]{"Expression | Mathematical phrase", "Biểu thức | Cụm từ toán học"},
                    new String[]{"Equation | Mathematical statement", "Phương trình | Tuyên bố toán học"},
                    new String[]{"Function | Relation between sets", "Hàm số | Mối quan hệ giữa tập hợp"},
                    new String[]{"Domain | Input values", "Miền xác định | Giá trị đầu vào"},
                    new String[]{"Range | Output values", "Miền giá trị | Giá trị đầu ra"},
                    new String[]{"Solve | Find the solution", "Giải | Tìm giải pháp"}
            );
            case "MATH-GEOMETRY" -> List.of(
                    new String[]{"Point | Zero-dimensional object", "Điểm | Đối tượng không chiều"},
                    new String[]{"Line | One-dimensional object", "Đường thẳng | Đối tượng một chiều"},
                    new String[]{"Plane | Two-dimensional object", "Mặt phẳng | Đối tượng hai chiều"},
                    new String[]{"Triangle | Three-sided polygon", "Tam giác | Đa giác ba cạnh"},
                    new String[]{"Square | Four-sided regular", "Hình vuông | Hình chữ nhật đều"},
                    new String[]{"Circle | Round shape", "Vòng tròn | Hình tròn"},
                    new String[]{"Area | Surface measure", "Diện tích | Đo bề mặt"},
                    new String[]{"Perimeter | Boundary length", "Chu vi | Độ dài biên"},
                    new String[]{"Volume | Space inside", "Thể tích | Không gian bên trong"},
                    new String[]{"Angle | Space between rays", "Góc | Không gian giữa tia"}
            );
            case "MATH-CALCULUS" -> List.of(
                    new String[]{"Derivative | Rate of change", "Đạo hàm | Tốc độ thay đổi"},
                    new String[]{"Integral | Accumulation", "Tích phân | Tích lũy"},
                    new String[]{"Limit | Approaching value", "Giới hạn | Giá trị tiếp cận"},
                    new String[]{"Continuous | No breaks", "Liên tục | Không có gián đoạn"},
                    new String[]{"Differentiable | Can take derivative", "Có đạo hàm | Có thể lấy đạo hàm"},
                    new String[]{"Chain rule | Composite derivative", "Quy tắc chuỗi | Đạo hàm hỗn hợp"},
                    new String[]{"Product rule | Derivative of product", "Quy tắc tích | Đạo hàm tích"},
                    new String[]{"Quotient rule | Derivative of quotient", "Quy tắc thương | Đạo hàm thương"},
                    new String[]{"Gradient | Multi-variable derivative", "Gradient | Đạo hàm nhiều biến"},
                    new String[]{"Taylor series | Function approximation", "Chuỗi Taylor | Xấp xỉ hàm số"}
            );
            case "SCI-BIOLOGY" -> List.of(
                    new String[]{"Cell | Basic unit of life", "Tế bào | Đơn vị cơ bản của sự sống"},
                    new String[]{"Mitochondria | Energy producer", "Ty thể | Nhà máy sản xuất năng lượng"},
                    new String[]{"Nucleus | Cell control center", "Nhân | Trung tâm kiểm soát tế bào"},
                    new String[]{"DNA | Genetic material", "DNA | Vật liệu di truyền"},
                    new String[]{"Protein | Building block", "Protein | Khối xây dựng"},
                    new String[]{"Organism | Living thing", "Sinh vật | Vật sống"},
                    new String[]{"Evolution | Change over time", "Tiến hóa | Thay đổi theo thời gian"},
                    new String[]{"Gene | Hereditary unit", "Gen | Đơn vị di truyền"},
                    new String[]{"Photosynthesis | Light energy conversion", "Quang hợp | Chuyển đổi năng lượng ánh sáng"},
                    new String[]{"Respiration | Energy release process", "Hô hấp | Quá trình phát hành năng lượng"}
            );
            case "SCI-CHEMISTRY" -> List.of(
                    new String[]{"Atom | Smallest unit of element", "Nguyên tử | Đơn vị nhỏ nhất của nguyên tố"},
                    new String[]{"Molecule | Group of atoms", "Phân tử | Nhóm nguyên tử"},
                    new String[]{"Element | Pure substance", "Nguyên tố | Chất tinh khiết"},
                    new String[]{"Compound | Two or more elements", "Hợp chất | Hai hay nhiều nguyên tố"},
                    new String[]{"Acid | pH less than 7", "Axit | pH dưới 7"},
                    new String[]{"Base | pH more than 7", "Base | pH trên 7"},
                    new String[]{"Reaction | Chemical change", "Phản ứng | Sự thay đổi hóa học"},
                    new String[]{"Bond | Force between atoms", "Liên kết | Lực giữa nguyên tử"},
                    new String[]{"Valence | Bonding capacity", "Hóa trị | Khả năng liên kết"},
                    new String[]{"Ion | Charged particle", "Ion | Hạt tích điện"}
            );
            case "HIST-ANCIENT" -> List.of(
                    new String[]{"Mesopotamia | Ancient region", "Lưỡng Hà | Vùng cổ đại"},
                    new String[]{"Pharaoh | Egyptian ruler", "Pharaoh | Người cai trị Ai Cập"},
                    new String[]{"Pyramid | Egyptian monument", "Lăng mộ | Di tích Ai Cập"},
                    new String[]{"Roman Empire | Ancient power", "Đế chế La Mã | Thế lực cổ đại"},
                    new String[]{"Caesar | Roman leader", "Caesar | Nhà lãnh đạo La Mã"},
                    new String[]{"Constantine | First Christian emperor", "Constantine | Hoàng đế Thiên chúa đầu tiên"},
                    new String[]{"Great Wall | Chinese defense", "Vạn Lý Trường Thành | Phòng thủ Trung Quốc"},
                    new String[]{"Dynasty | Ruling family", "Triều đại | Gia đình cai trị"},
                    new String[]{"Civilization | Advanced society", "Nền văn minh | Xã hội tiên tiến"},
                    new String[]{"Barbarian | Non-civilized people", "Dân man rợ | Nhân dân bất kỳ"}
            );
            case "HIST-MEDIEVAL" -> List.of(
                    new String[]{"Knight | Feudal warrior", "Hiệp sĩ | Chiến binh phong kiến"},
                    new String[]{"Feudalism | Social system", "Chế độ phong kiến | Hệ thống xã hội"},
                    new String[]{"Crusade | Religious war", "Chiến thánh chiến | Cuộc chiến tôn giáo"},
                    new String[]{"Castle | Fortified residence", "Lâu đài | Nhà ở được kiên cố"},
                    new String[]{"Black Death | Plague pandemic", "Dịch Đen | Dịch bệnh lây lan"},
                    new String[]{"Renaissance | Cultural rebirth", "Phục Hưng | Tái sinh văn hóa"},
                    new String[]{"Guild | Professional association", "Hội thủ công | Hiệp hội chuyên nghiệp"},
                    new String[]{"Monarch | King or queen", "Vua | Vua hoặc nữ hoàng"},
                    new String[]{"Peasant | Farm worker", "Nông dân | Lao động nông thôn"},
                    new String[]{"Monastery | Religious community", "Tu viện | Cộng đồng tôn giáo"}
            );
            case "HIST-MODERN" -> List.of(
                    new String[]{"Renaissance | 14-17th century revival", "Phục Hưng | Thời kỳ hồi sinh 14-17 thế kỷ"},
                    new String[]{"Industrial Revolution | Machine age", "Cách mạng Công nghiệp | Thời đại máy móc"},
                    new String[]{"American Revolution | 1776 independence", "Cách mạng Mỹ | Độc lập năm 1776"},
                    new String[]{"French Revolution | 1789 reform", "Cách mạng Pháp | Cải cách năm 1789"},
                    new String[]{"Enlightenment | Age of reason", "Thời Trí tuệ | Thời đại lý trí"},
                    new String[]{"Imperialism | Empire building", "Chủ nghĩa đế quốc | Xây dựng đế chế"},
                    new String[]{"Victorian Era | Queen Victoria's reign", "Thời đại Victoria | Triều đại Nữ hoàng Victoria"},
                    new String[]{"World War I | 1914-1918", "Chiến tranh Thế giới I | 1914-1918"},
                    new String[]{"World War II | 1939-1945", "Chiến tranh Thế giới II | 1939-1945"},
                    new String[]{"Cold War | US-Soviet tension", "Chiến tranh Lạnh | Căng thẳng Mỹ-Xô"}
            );
            default -> getGenericCards(deckCode); // Fallback for private/unknown decks
        };
    }

    private List<String[]> getGenericCards(String deckCode) {
        // Generic cards for private or unknown decks
        List<String[]> cards = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            String index = String.format(Locale.ROOT, "%02d", i);
            cards.add(new String[]{
                    deckCode + " - Question " + index,
                    deckCode + " - Answer " + index
            });
        }
        return cards;
    }

    private record UserSeed(String email, UserRole role, boolean verified, boolean banned) {
    }

    private record DeckSeed(String code, String ownerEmail, boolean isPublic, String topic, String description) {
    }
}
