package com.fairfold.resume;

import com.fairfold.common.errors.ApiException;
import com.fairfold.common.storage.StorageService;
import com.fairfold.resume.dto.ResumeResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class ResumeService {

    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;
    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

    private final ResumeRepository resumeRepository;
    private final ResumePlaceholderRepository placeholderRepository;
    private final StorageService storageService;
    private final ResumeProcessingService processingService;
    private final ResumeTextExtractor extractor;

    public ResumeService(ResumeRepository resumeRepository,
                         ResumePlaceholderRepository placeholderRepository,
                         StorageService storageService,
                         ResumeProcessingService processingService,
                         ResumeTextExtractor extractor) {
        this.resumeRepository = resumeRepository;
        this.placeholderRepository = placeholderRepository;
        this.storageService = storageService;
        this.processingService = processingService;
        this.extractor = extractor;
    }

    @Transactional
    public ResumeResponse upload(UUID userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("INVALID_FILE", "Upload a file.");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw ApiException.badRequest("FILE_TOO_LARGE", "Resume must be at most 10 MB.");
        }
        byte[] content = readBytes(file);
        if (!isPdf(content)) {
            throw ApiException.badRequest("INVALID_FILE", "Resume must be a PDF file.");
        }

        String storageKey = storageService.save(content, file.getOriginalFilename());

        // A new upload replaces the current resume.
        List<Resume> current = resumeRepository.findByUserIdAndCurrentTrueOrderByCreatedAtDesc(userId);
        for (Resume previous : current) {
            previous.setCurrent(false);
            previous.setCurrentUserKey(null);
        }
        // Flush the NULLs now; otherwise Hibernate inserts the new row before
        // clearing the old unique key, violating uq_resumes_current_user.
        resumeRepository.flush();

        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setOriginalFilename(file.getOriginalFilename());
        resume.setStorageKey(storageKey);
        resume.setSizeBytes(content.length);
        resume.setStatus(ResumeStatus.processing);
        resume.setCurrent(true);
        resume.setCurrentUserKey(userId);
        resumeRepository.save(resume);

        UUID resumeId = resume.getId();
        afterCommit(() -> processingService.process(resumeId));
        return toResponse(resume);
    }

    @Transactional(readOnly = true)
    public ResumeResponse getCurrent(UUID userId) {
        Resume resume = resumeRepository.findByUserIdAndCurrentTrue(userId)
                .orElseThrow(() -> ApiException.notFound("RESUME_NOT_FOUND", "No resume uploaded yet."));
        return toResponse(resume);
    }

    @Transactional(readOnly = true)
    public DownloadedResume loadCurrentFile(UUID userId) {
        Resume resume = resumeRepository.findByUserIdAndCurrentTrue(userId)
                .orElseThrow(() -> ApiException.notFound("RESUME_NOT_FOUND", "No resume uploaded yet."));
        byte[] bytes = storageService.load(resume.getStorageKey());
        String filename = resume.getOriginalFilename() != null ? resume.getOriginalFilename() : "resume.pdf";
        return new DownloadedResume(filename, bytes);
    }

    public record DownloadedResume(String filename, byte[] bytes) {}

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    private boolean isPdf(byte[] content) {
        if (content.length < PDF_SIGNATURE.length) {
            return false;
        }
        for (int i = 0; i < PDF_SIGNATURE.length; i++) {
            if (content[i] != PDF_SIGNATURE[i]) {
                return false;
            }
        }
        return true;
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw ApiException.badRequest("INVALID_FILE", "Could not read the uploaded file.");
        }
    }

    private ResumeResponse toResponse(Resume resume) {
        String failureCode = resume.getFailureReason();
        List<ResumeResponse.DroppedPlaceholder> placeholders =
                placeholderRepository.findByResumeIdOrderByPlaceholderAsc(resume.getId()).stream()
                        .map(row -> new ResumeResponse.DroppedPlaceholder(
                                row.getPlaceholder(), row.getKind(), row.getOriginalValue()))
                        .toList();
        return new ResumeResponse(
                resume.getId(), resume.getOriginalFilename(), resume.getSizeBytes(),
                resume.getStatus().name(), failureCode,
                failureCode == null ? null : extractor.messageFor(failureCode),
                resume.getStrippedText(), placeholders, resume.getCreatedAt());
    }
}
