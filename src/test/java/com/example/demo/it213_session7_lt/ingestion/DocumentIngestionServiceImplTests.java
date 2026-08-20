package com.example.demo.it213_session7_lt.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

@ExtendWith(MockitoExtension.class)
class DocumentIngestionServiceImplTests {

    @Mock
    private TikaDocumentLoader loader;
    @Mock
    private IngestionRegistryRepository registry;
    @Mock
    private VectorStore vectorStore;

    private DocumentIngestionServiceImpl service;
    private Resource resource;

    @BeforeEach
    void setUp() {
        service = new DocumentIngestionServiceImpl(
                loader, new TokenWindowChunker(40, 10), registry, vectorStore);
        resource = new ByteArrayResource(
                "Thông tin bảo hành LapTech".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "store.txt";
            }
        };
    }

    @Test
    void ingestsNewDocumentAndMarksItComplete() {
        when(registry.reserve(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq("store.txt"))).thenReturn(true);
        when(loader.load(resource)).thenReturn(List.of(new Document(
                "Laptop được bảo hành chính hãng từ 12 đến 24 tháng.")));

        IngestionResult result = service.ingest(resource);

        assertThat(result.status()).isEqualTo(IngestionResult.Status.INGESTED);
        assertThat(result.chunkCount()).isPositive();
        assertThat(result.contentHash()).hasSize(64);
        verify(vectorStore).add(anyList());
        verify(registry).markComplete(result.contentHash(), result.chunkCount());
    }

    @Test
    void skipsDocumentWhenItsHashWasAlreadyReserved() {
        when(registry.reserve(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq("store.txt"))).thenReturn(false);

        IngestionResult result = service.ingest(resource);

        assertThat(result.status()).isEqualTo(IngestionResult.Status.ALREADY_INGESTED);
        assertThat(result.chunkCount()).isZero();
        verify(loader, never()).load(resource);
        verify(vectorStore, never()).add(anyList());
    }
}
