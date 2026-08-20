package com.example.demo.it213_session7_lt.ingestion;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentIngestionServiceImpl implements DocumentIngestionService {

    private final TikaDocumentLoader documentLoader;
    private final TokenWindowChunker chunker;
    private final IngestionRegistryRepository registry;
    private final VectorStore vectorStore;

    public DocumentIngestionServiceImpl(TikaDocumentLoader documentLoader,
                                        TokenWindowChunker chunker,
                                        IngestionRegistryRepository registry,
                                        VectorStore vectorStore) {
        this.documentLoader = documentLoader;
        this.chunker = chunker;
        this.registry = registry;
        this.vectorStore = vectorStore;
    }

    @Override
    @Transactional
    public IngestionResult ingest(Resource resource) {
        validate(resource);
        String sourceName = resource.getFilename() == null ? resource.getDescription() : resource.getFilename();
        String contentHash = sha256(resource);

        if (!registry.reserve(contentHash, sourceName)) {
            return new IngestionResult(IngestionResult.Status.ALREADY_INGESTED,
                    sourceName, contentHash, 0);
        }

        var documents = documentLoader.load(resource);
        var chunks = chunker.split(documents, contentHash, sourceName);
        if (chunks.isEmpty()) {
            throw new IllegalArgumentException("The source document does not contain ingestible text");
        }

        vectorStore.add(chunks);
        registry.markComplete(contentHash, chunks.size());
        return new IngestionResult(IngestionResult.Status.INGESTED,
                sourceName, contentHash, chunks.size());
    }

    private static void validate(Resource resource) {
        if (resource == null || !resource.exists() || !resource.isReadable()) {
            throw new IllegalArgumentException("The configured source document cannot be read");
        }
    }

    private static String sha256(Resource resource) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = resource.getInputStream();
                 DigestInputStream ignored = new DigestInputStream(input, digest)) {
                ignored.transferTo(java.io.OutputStream.nullOutputStream());
            }
            return HexFormat.of().formatHex(digest.digest());
        }
        catch (IOException exception) {
            throw new UncheckedIOException("Cannot read the source document", exception);
        }
        catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
