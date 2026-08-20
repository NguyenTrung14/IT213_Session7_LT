package com.example.demo.it213_session7_lt.ingestion;

import org.springframework.core.io.Resource;

public interface DocumentIngestionService {

    IngestionResult ingest(Resource resource);
}
