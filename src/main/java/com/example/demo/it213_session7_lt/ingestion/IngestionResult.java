package com.example.demo.it213_session7_lt.ingestion;

public record IngestionResult(Status status, String source, String contentHash, int chunkCount) {

    public enum Status {
        INGESTED,
        ALREADY_INGESTED
    }
}
