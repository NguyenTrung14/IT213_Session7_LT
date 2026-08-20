package com.example.demo.it213_session7_lt.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class LapTechPdfPipelineTests {

    @Test
    void extractsAndChunksTheBundledLapTechPdf() {
        var resource = new ClassPathResource("docs/LapTech_Store_Info.pdf");
        var documents = new TikaDocumentLoader().load(resource);
        var chunks = new TokenWindowChunker(350, 60).split(
                documents, "b".repeat(64), resource.getFilename());

        assertThat(documents).isNotEmpty();
        assertThat(documents).extracting(document -> document.getText())
                .anySatisfy(text -> assertThat(text)
                        .contains("THÔNG TIN CỬA HÀNG LAPTECH")
                        .contains("Chính sách bảo hành"));
        assertThat(chunks).hasSizeGreaterThan(2);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.getMetadata())
                .containsEntry("source", "LapTech_Store_Info.pdf"));
    }
}
