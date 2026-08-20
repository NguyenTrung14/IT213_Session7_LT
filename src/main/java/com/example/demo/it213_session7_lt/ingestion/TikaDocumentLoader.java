package com.example.demo.it213_session7_lt.ingestion;

import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class TikaDocumentLoader {

    public List<Document> load(Resource resource) {
        return new TikaDocumentReader(resource).get();
    }
}
