package com.example.demo.it213_session7_lt.ingestion;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;
import com.example.demo.it213_session7_lt.config.RagProperties;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

@Component
public class TokenWindowChunker {

    private final int chunkSize;
    private final int overlap;
    private final Encoding encoding;

    public TokenWindowChunker(RagProperties properties) {
        this(properties.chunkSize(), properties.chunkOverlap());
    }

    TokenWindowChunker(int chunkSize, int overlap) {
        if (chunkSize <= 0 || overlap < 0 || overlap >= chunkSize) {
            throw new IllegalArgumentException("Invalid chunk size or overlap");
        }
        this.chunkSize = chunkSize;
        this.overlap = overlap;
        this.encoding = Encodings.newLazyEncodingRegistry().getEncoding(EncodingType.CL100K_BASE);
    }

    public List<Document> split(List<Document> documents, String contentHash, String sourceName) {
        List<Document> chunks = new ArrayList<>();
        int chunkIndex = 0;

        for (Document document : documents) {
            String text = document.getText();
            if (text == null || text.isBlank()) {
                continue;
            }

            IntArrayList tokens = encoding.encode(text.strip());
            int step = chunkSize - overlap;
            for (int start = 0; start < tokens.size(); start += step) {
                int end = Math.min(start + chunkSize, tokens.size());
                IntArrayList window = copy(tokens, start, end);
                var metadata = new HashMap<String, Object>(document.getMetadata());
                metadata.put("source", sourceName);
                metadata.put("contentHash", contentHash);
                metadata.put("chunkIndex", chunkIndex);
                metadata.put("chunkSize", chunkSize);
                metadata.put("chunkOverlap", overlap);

                String idSeed = contentHash + ":" + chunkIndex;
                String chunkId = UUID.nameUUIDFromBytes(idSeed.getBytes(StandardCharsets.UTF_8)).toString();
                chunks.add(Document.builder()
                        .id(chunkId)
                        .text(encoding.decode(window).strip())
                        .metadata(metadata)
                        .build());
                chunkIndex++;

                if (end == tokens.size()) {
                    break;
                }
            }
        }

        return chunks;
    }

    int countTokens(String text) {
        return encoding.countTokens(text);
    }

    private static IntArrayList copy(IntArrayList tokens, int start, int end) {
        IntArrayList copy = new IntArrayList(end - start);
        for (int index = start; index < end; index++) {
            copy.add(tokens.get(index));
        }
        return copy;
    }
}
