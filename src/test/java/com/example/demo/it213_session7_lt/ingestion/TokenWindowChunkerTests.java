package com.example.demo.it213_session7_lt.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

class TokenWindowChunkerTests {

    @Test
    void splitsTextIntoBoundedOverlappingChunksAndAddsMetadata() {
        var chunker = new TokenWindowChunker(40, 10);
        String text = "LapTech hỗ trợ khách hàng. ".repeat(80);

        var chunks = chunker.split(
                List.of(new Document(text)),
                "a".repeat(64),
                "store.txt");

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk -> {
            assertThat(chunker.countTokens(chunk.getText())).isLessThanOrEqualTo(40);
            assertThat(chunk.getMetadata())
                    .containsEntry("source", "store.txt")
                    .containsEntry("chunkSize", 40)
                    .containsEntry("chunkOverlap", 10);
        });
        assertThat(chunks).extracting(chunk -> chunk.getMetadata().get("chunkIndex"))
                .containsExactlyElementsOf(java.util.stream.IntStream.range(0, chunks.size())
                        .boxed().toList());
    }
}
