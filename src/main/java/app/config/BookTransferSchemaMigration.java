package app.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BookTransferSchemaMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BookTransferSchemaMigration.class);

    private final JdbcTemplate jdbcTemplate;

    public BookTransferSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!bookTransferTableExists()) {
            return;
        }

        migrateColumnIndex("sender_id");
        migrateColumnIndex("receiver_id");
    }

    private void migrateColumnIndex(String columnName) {
        List<String> uniqueIndexes = findUniqueIndexes(columnName);
        if (uniqueIndexes.isEmpty()) {
            return;
        }

        List<ForeignKeyDetail> foreignKeys = findForeignKeys(columnName);

        try {
            for (ForeignKeyDetail foreignKey : foreignKeys) {
                jdbcTemplate.execute("ALTER TABLE book_transfer DROP FOREIGN KEY `" + foreignKey.name() + "`");
                log.info("Dropped foreign key {} on book_transfer.{}", foreignKey.name(), columnName);
            }

            for (String indexName : uniqueIndexes) {
                jdbcTemplate.execute("ALTER TABLE book_transfer DROP INDEX `" + indexName + "`");
                log.info("Dropped legacy unique index {} on book_transfer.{}", indexName, columnName);
            }

            if (!hasIndexOnColumn(columnName)) {
                String indexName = "idx_book_transfer_" + columnName;
                jdbcTemplate.execute("ALTER TABLE book_transfer ADD INDEX `" + indexName + "` (`" + columnName + "`)");
                log.info("Added non-unique index {} on book_transfer", indexName);
            }

            for (ForeignKeyDetail foreignKey : foreignKeys) {
                jdbcTemplate.execute(
                        "ALTER TABLE book_transfer ADD CONSTRAINT `" + foreignKey.name()
                                + "` FOREIGN KEY (`" + columnName + "`) REFERENCES `"
                                + foreignKey.referencedTable() + "` (`" + foreignKey.referencedColumn() + "`)");
                log.info("Recreated foreign key {} on book_transfer.{}", foreignKey.name(), columnName);
            }
        } catch (RuntimeException ex) {
            log.warn("Could not migrate book_transfer.{} indexes: {}", columnName, ex.getMessage());
        }
    }

    private List<String> findUniqueIndexes(String columnName) {
        return jdbcTemplate.queryForList("""
                SELECT DISTINCT INDEX_NAME
                FROM INFORMATION_SCHEMA.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'book_transfer'
                  AND COLUMN_NAME = ?
                  AND NON_UNIQUE = 0
                  AND INDEX_NAME <> 'PRIMARY'
                """, String.class, columnName);
    }

    private List<ForeignKeyDetail> findForeignKeys(String columnName) {
        return jdbcTemplate.query("""
                SELECT CONSTRAINT_NAME,
                       MAX(REFERENCED_TABLE_NAME) AS REFERENCED_TABLE_NAME,
                       MAX(REFERENCED_COLUMN_NAME) AS REFERENCED_COLUMN_NAME
                FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'book_transfer'
                  AND COLUMN_NAME = ?
                  AND REFERENCED_TABLE_NAME IS NOT NULL
                GROUP BY CONSTRAINT_NAME
                """,
                (rs, rowNum) -> new ForeignKeyDetail(
                        rs.getString("CONSTRAINT_NAME"),
                        rs.getString("REFERENCED_TABLE_NAME"),
                        rs.getString("REFERENCED_COLUMN_NAME")
                ),
                columnName
        );
    }

    private boolean hasIndexOnColumn(String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT INDEX_NAME)
                FROM INFORMATION_SCHEMA.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'book_transfer'
                  AND COLUMN_NAME = ?
                """, Integer.class, columnName);
        return count != null && count > 0;
    }

    private boolean bookTransferTableExists() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'book_transfer'
                """, Integer.class);
        return count != null && count > 0;
    }

    private record ForeignKeyDetail(String name, String referencedTable, String referencedColumn) {
    }
}
