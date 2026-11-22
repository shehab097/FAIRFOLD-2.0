package com.fairfold.resume;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.Normalizer;

/**
 * Extracts text from a PDF and rejects resumes that cannot be screened:
 * too little text (scanned) or mostly non-Latin letters (not English).
 */
@Component
public class ResumeTextExtractor {

    public static final String SCANNED_OR_EMPTY = "SCANNED_OR_EMPTY";
    public static final String NOT_ENGLISH = "NOT_ENGLISH";
    public static final String UNREADABLE = "UNREADABLE";

    private static final int MIN_CHARACTERS = 200;
    private static final double MIN_LATIN_RATIO = 0.85;

    public String extract(byte[] pdf) {
        String text;
        try (PDDocument document = Loader.loadPDF(pdf)) {
            text = new PDFTextStripper().getText(document);
        } catch (IOException | RuntimeException e) {
            throw new ResumeExtractionException(UNREADABLE,
                    "We could not read this PDF. Try uploading it again.");
        }
        return validate(text);
    }

    /** Visible for unit tests: applies the length and Latin-letter rules. */
    public String validate(String text) {
        String clean = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFC).strip();
        if (clean.length() < MIN_CHARACTERS) {
            throw new ResumeExtractionException(SCANNED_OR_EMPTY,
                    "This resume has too little readable text. Upload a text-based PDF rather than a scan.");
        }
        long letters = 0;
        long latin = 0;
        for (int i = 0; i < clean.length(); ) {
            int codePoint = clean.codePointAt(i);
            if (Character.isLetter(codePoint)) {
                letters++;
                if (Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.LATIN) {
                    latin++;
                }
            }
            i += Character.charCount(codePoint);
        }
        if (letters == 0 || (double) latin / letters < MIN_LATIN_RATIO) {
            throw new ResumeExtractionException(NOT_ENGLISH,
                    "This resume does not look like an English resume. Upload an English PDF.");
        }
        return clean;
    }

    public String messageFor(String code) {
        if (SCANNED_OR_EMPTY.equals(code)) {
            return "This resume has too little readable text. Upload a text-based PDF rather than a scan.";
        }
        if (NOT_ENGLISH.equals(code)) {
            return "This resume does not look like an English resume. Upload an English PDF.";
        }
        if ("IDENTIFIER_LEAK".equals(code)) {
            return "Processing stopped because personal details were still present. Report the strip and try again.";
        }
        return "We could not process this resume. Try uploading it again.";
    }

    public boolean isEnglish(String text) {
        try {
            validate(text);
            return true;
        } catch (ResumeExtractionException e) {
            return !NOT_ENGLISH.equals(e.getCode());
        }
    }
}
