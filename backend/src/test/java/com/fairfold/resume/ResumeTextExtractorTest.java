package com.fairfold.resume;

import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResumeTextExtractorTest {

    private final ResumeTextExtractor extractor = new ResumeTextExtractor();

    @Test
    void Fr012_extracts_text_from_normal_pdf() throws Exception {
        String text = extractor.extract(read("/resumes/pdf/normal_1.pdf"));
        assertThat(text).contains("Software Engineer").contains("Java");
    }

    @Test
    void Fr012_image_only_pdfs_fail_as_scanned() throws Exception {
        for (String file : new String[]{"image_only_1.pdf", "image_only_2.pdf"}) {
            assertThatThrownBy(() -> extractor.extract(read("/resumes/pdf/" + file)))
                    .isInstanceOf(ResumeExtractionException.class)
                    .hasMessageContaining("readable text")
                    .extracting(e -> ((ResumeExtractionException) e).getCode())
                    .isEqualTo(ResumeTextExtractor.SCANNED_OR_EMPTY);
        }
    }

    @Test
    void Fr012_bangla_pdf_fails_as_not_english() throws Exception {
        assertThatThrownBy(() -> extractor.extract(read("/resumes/pdf/bangla.pdf")))
                .isInstanceOf(ResumeExtractionException.class)
                .extracting(e -> ((ResumeExtractionException) e).getCode())
                .isEqualTo(ResumeTextExtractor.NOT_ENGLISH);
    }

    @Test
    void Fr012_short_text_is_scanned_or_empty() {
        assertThatThrownBy(() -> extractor.validate("Too short"))
                .isInstanceOf(ResumeExtractionException.class)
                .extracting(e -> ((ResumeExtractionException) e).getCode())
                .isEqualTo(ResumeTextExtractor.SCANNED_OR_EMPTY);
    }

    @Test
    void Fr012_low_latin_ratio_is_not_english() {
        String greek = "αβγδεζηθικλμνξοπρστυφχψω".repeat(20);
        assertThatThrownBy(() -> extractor.validate(greek))
                .isInstanceOf(ResumeExtractionException.class)
                .extracting(e -> ((ResumeExtractionException) e).getCode())
                .isEqualTo(ResumeTextExtractor.NOT_ENGLISH);
    }

    @Test
    void Fr012_long_english_text_passes() {
        String text = "Java Spring Boot developer with five years of experience building APIs. ".repeat(10);
        assertThat(extractor.validate(text)).contains("Java");
    }

    private byte[] read(String resource) throws Exception {
        try (InputStream stream = getClass().getResourceAsStream(resource)) {
            assertThat(stream).as(resource).isNotNull();
            return stream.readAllBytes();
        }
    }
}
