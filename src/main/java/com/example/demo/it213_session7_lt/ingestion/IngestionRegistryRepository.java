package com.example.demo.it213_session7_lt.ingestion;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class IngestionRegistryRepository {

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS rag_ingested_document (
                content_hash VARCHAR(64) PRIMARY KEY,
                source_name VARCHAR(255) NOT NULL,
                chunk_count INTEGER NOT NULL DEFAULT 0,
                ingested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """;

    private final JdbcClient jdbcClient;

    public IngestionRegistryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public boolean reserve(String contentHash, String sourceName) {
        jdbcClient.sql(CREATE_TABLE_SQL).update();
        int inserted = jdbcClient.sql("""
                        INSERT INTO rag_ingested_document(content_hash, source_name)
                        VALUES (:contentHash, :sourceName)
                        ON CONFLICT (content_hash) DO NOTHING
                        """)
                .param("contentHash", contentHash)
                .param("sourceName", sourceName)
                .update();
        return inserted == 1;
    }

    public void markComplete(String contentHash, int chunkCount) {
        jdbcClient.sql("""
                        UPDATE rag_ingested_document
                        SET chunk_count = :chunkCount, ingested_at = CURRENT_TIMESTAMP
                        WHERE content_hash = :contentHash
                        """)
                .param("contentHash", contentHash)
                .param("chunkCount", chunkCount)
                .update();
    }
}
