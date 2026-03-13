package com.khaleo.flashcard.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.khaleo.flashcard.integration.support.IntegrationPersistenceTestBase;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class FlywaySchemaContractTest extends IntegrationPersistenceTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldCreateRequiredRelationalTables() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()",
                String.class);

        assertThat(tables)
                .contains("users", "decks", "cards", "card_learning_states");
    }

    @Test
    void shouldCreateUniqueConstraintForUserCardLearningState() {
        Integer uniqueIndexes = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = 'card_learning_states'
                  AND index_name = 'uk_learning_state_user_card'
                """,
                Integer.class);

        assertThat(uniqueIndexes).isNotNull();
        assertThat(uniqueIndexes).isGreaterThan(0);
    }
}
