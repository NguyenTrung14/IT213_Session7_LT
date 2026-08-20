package com.example.demo.it213_session7_lt.ingestion;

import com.example.demo.it213_session7_lt.config.RagProperties;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminIngestionController {

    private final DocumentIngestionService ingestionService;
    private final ResourceLoader resourceLoader;
    private final RagProperties properties;

    public AdminIngestionController(DocumentIngestionService ingestionService,
                                    ResourceLoader resourceLoader,
                                    RagProperties properties) {
        this.ingestionService = ingestionService;
        this.resourceLoader = resourceLoader;
        this.properties = properties;
    }

    @PostMapping("/ingest-store-info")
    public ResponseEntity<IngestionResult> ingestStoreInfo() {
        var resource = resourceLoader.getResource(properties.documentLocation());
        var result = ingestionService.ingest(resource);
        HttpStatus status = result.status() == IngestionResult.Status.INGESTED
                ? HttpStatus.CREATED
                : HttpStatus.OK;
        return ResponseEntity.status(status).body(result);
    }
}
