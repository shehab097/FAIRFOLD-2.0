package com.fairfold.coaching;

import com.fairfold.ai.AiGateway;
import com.fairfold.ai.AiResult;
import com.fairfold.ai.StarFeedback;
import com.fairfold.candidate.CandidateProfile;
import com.fairfold.candidate.CandidateProfileRepository;
import com.fairfold.coaching.dto.PracticeQuestionDto;
import com.fairfold.coaching.dto.PracticeQuestionOption;
import com.fairfold.coaching.dto.PracticeQuestionRequest;
import com.fairfold.coaching.dto.PracticeSessionListItem;
import com.fairfold.coaching.dto.PracticeSessionRequest;
import com.fairfold.coaching.dto.PracticeSessionResponse;
import com.fairfold.common.errors.ApiException;
import com.fairfold.common.paging.PageResponse;
import com.fairfold.resume.IdentifierScan;
import com.fairfold.resume.ResumeStripper;
import com.fairfold.resume.StripTerm;
import com.fairfold.resume.StripTermRepository;
import com.fairfold.user.User;
import com.fairfold.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PracticeService {

    private static final String RUBRIC_VERSION = "star-v1";

    private final PracticeQuestionRepository questionRepository;
    private final PracticeSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final CandidateProfileRepository profileRepository;
    private final StripTermRepository stripTermRepository;
    private final AiGateway aiGateway;
    private final ObjectMapper objectMapper;
    private final ResumeStripper stripper = new ResumeStripper();

    public PracticeService(PracticeQuestionRepository questionRepository,
                           PracticeSessionRepository sessionRepository,
                           UserRepository userRepository,
                           CandidateProfileRepository profileRepository,
                           StripTermRepository stripTermRepository,
                           AiGateway aiGateway,
                           ObjectMapper objectMapper) {
        this.questionRepository = questionRepository;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.stripTermRepository = stripTermRepository;
        this.aiGateway = aiGateway;
        this.objectMapper = objectMapper;
    }

    // --- admin bank --------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<PracticeQuestionDto> listQuestions(Pageable pageable) {
        Page<PracticeQuestion> page = questionRepository.findAllByOrderByTopicAscTextAsc(pageable);
        return PageResponse.of(page.getContent().stream().map(this::toQuestionDto).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional
    public PracticeQuestionDto createQuestion(PracticeQuestionRequest request) {
        PracticeQuestion question = new PracticeQuestion();
        applyQuestion(question, request);
        questionRepository.save(question);
        return toQuestionDto(question);
    }

    @Transactional
    public PracticeQuestionDto updateQuestion(UUID id, PracticeQuestionRequest request) {
        PracticeQuestion question = requireQuestion(id);
        applyQuestion(question, request);
        questionRepository.save(question);
        return toQuestionDto(question);
    }

    @Transactional
    public PracticeQuestionDto setQuestionActive(UUID id, boolean active) {
        PracticeQuestion question = requireQuestion(id);
        question.setActive(active);
        questionRepository.save(question);
        return toQuestionDto(question);
    }

    // --- candidate practice ------------------------------------------------

    @Transactional(readOnly = true)
    public List<PracticeQuestionOption> questions(String topic) {
        List<PracticeQuestion> questions = topic == null || topic.isBlank()
                ? questionRepository.findByActiveTrueOrderByTopicAscTextAsc()
                : questionRepository.findByActiveTrueAndTopicIgnoreCaseOrderByTextAsc(topic.strip());
        return questions.stream()
                .map(q -> new PracticeQuestionOption(q.getId(), q.getText(), q.getTopic()))
                .toList();
    }

    @Transactional
    public PracticeSessionResponse createSession(UUID userId, PracticeSessionRequest request) {
        PracticeQuestion question = questionRepository.findById(request.questionId())
                .filter(PracticeQuestion::isActive)
                .orElseThrow(() -> ApiException.notFound("QUESTION_NOT_FOUND",
                        "Practice question not found."));
        String answer = request.answer().strip();
        if (answer.length() < 20 || answer.length() > 3000) {
            throw ApiException.badRequest("INVALID_ANSWER",
                    "The answer must be between 20 and 3000 characters.");
        }

        User user = userRepository.findById(userId).orElseThrow(() ->
                ApiException.unauthorized("UNAUTHORIZED", "Authentication required."));
        CandidateProfile profile = profileRepository.findById(userId).orElse(null);
        ResumeStripper.Identifiers identifiers = new ResumeStripper.Identifiers(
                user.getFirstName(), user.getLastName(), user.getEmail(),
                profile == null ? null : profile.getPhone());
        ResumeStripper.Terms terms = loadTerms();
        ResumeStripper.Result anonymised = stripper.anonymise(answer, identifiers, terms);

        StarFeedback feedback = null;
        String provider = null;
        if (!IdentifierScan.leaks(anonymised.text(), identifiers)) {
            try {
                AiResult<StarFeedback> result = aiGateway.gradeStar(
                        userId, question.getText(), anonymised.text());
                feedback = validate(result.value(), anonymised.text(), anonymised.mappings());
                provider = result.model();
            } catch (RuntimeException e) {
                // No fallback: keep the answer, report that feedback is unavailable.
                feedback = null;
                provider = null;
            }
        }

        PracticeSession session = new PracticeSession();
        session.setUserId(userId);
        session.setQuestionId(question.getId());
        session.setAnswer(answer);
        session.setRubricVersion(RUBRIC_VERSION);
        session.setProvider(provider);
        session.setFeedback(feedback == null ? null : write(feedback));
        sessionRepository.save(session);
        return toSessionResponse(session, question, feedback);
    }

    @Transactional(readOnly = true)
    public PageResponse<PracticeSessionListItem> history(UUID userId, Pageable pageable) {
        Page<PracticeSession> page = sessionRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);
        Map<UUID, String> questions = questionTexts(page.getContent());
        return PageResponse.of(page.getContent().stream()
                .map(session -> new PracticeSessionListItem(
                        session.getId(), session.getQuestionId(),
                        questions.get(session.getQuestionId()), topicOf(session.getQuestionId()),
                        session.getRating(), session.getFeedback() != null, session.getCreatedAt()))
                .toList(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PracticeSessionResponse session(UUID userId, UUID id) {
        PracticeSession session = requireSession(userId, id);
        PracticeQuestion question = questionRepository.findById(session.getQuestionId()).orElse(null);
        return toSessionResponse(session, question, read(session.getFeedback()));
    }

    @Transactional
    public PracticeSessionResponse rate(UUID userId, UUID id, int rating) {
        if (rating < 1 || rating > 5) {
            throw ApiException.badRequest("INVALID_RATING", "Rating must be between 1 and 5.");
        }
        PracticeSession session = requireSession(userId, id);
        session.setRating(rating);
        sessionRepository.save(session);
        PracticeQuestion question = questionRepository.findById(session.getQuestionId()).orElse(null);
        return toSessionResponse(session, question, read(session.getFeedback()));
    }

    // --- helpers -----------------------------------------------------------

    private PracticeQuestion requireQuestion(UUID id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("QUESTION_NOT_FOUND",
                        "Practice question not found."));
    }

    private PracticeSession requireSession(UUID userId, UUID id) {
        return sessionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("SESSION_NOT_FOUND",
                        "Practice session not found."));
    }

    private void applyQuestion(PracticeQuestion question, PracticeQuestionRequest request) {
        question.setText(request.text().strip());
        question.setTopic(request.topic().strip());
        question.setActive(request.active() == null || request.active());
    }

    private PracticeQuestionDto toQuestionDto(PracticeQuestion question) {
        return new PracticeQuestionDto(question.getId(), question.getText(),
                question.getTopic(), question.isActive());
    }

    private StarFeedback validate(StarFeedback feedback, String anonymised,
                                  List<ResumeStripper.Mapping> mappings) {
        if (feedback == null) {
            return null;
        }
        Map<String, String> placeholders = new LinkedHashMap<>();
        for (ResumeStripper.Mapping mapping : mappings) {
            placeholders.put("[" + mapping.placeholder() + "]", mapping.originalValue());
        }
        return new StarFeedback(
                component(feedback.situation(), anonymised, placeholders),
                component(feedback.task(), anonymised, placeholders),
                component(feedback.action(), anonymised, placeholders),
                component(feedback.result(), anonymised, placeholders),
                restore(feedback.summary(), placeholders));
    }

    private StarFeedback.Component component(StarFeedback.Component component, String anonymised,
                                             Map<String, String> placeholders) {
        int rating = component == null ? 1 : Math.max(1, Math.min(5, component.rating()));
        List<StarFeedback.Point> points = new ArrayList<>();
        if (component != null) {
            String haystack = normalise(anonymised);
            for (StarFeedback.Point point : component.points()) {
                if (point == null || point.quote() == null || point.quote().isBlank()) {
                    continue;
                }
                String quote = point.quote().strip();
                if (!haystack.contains(normalise(quote))) {
                    continue; // invented or altered quote
                }
                points.add(new StarFeedback.Point(
                        restore(point.comment(), placeholders), restore(quote, placeholders)));
            }
        }
        return new StarFeedback.Component(rating, points);
    }

    private String normalise(String value) {
        return value == null ? "" : value.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").strip().replaceAll("\\s+", " ");
    }

    private String restore(String value, Map<String, String> placeholders) {
        if (value == null) {
            return null;
        }
        String restored = value;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            restored = restored.replace(entry.getKey(), entry.getValue());
        }
        return restored;
    }

    private ResumeStripper.Terms loadTerms() {
        List<String> institutions = stripTermRepository.findByKind("institution").stream()
                .map(StripTerm::getTerm).toList();
        List<String> employers = stripTermRepository.findByKind("employer").stream()
                .map(StripTerm::getTerm).toList();
        return new ResumeStripper.Terms(institutions, employers);
    }

    private Map<UUID, String> questionTexts(List<PracticeSession> sessions) {
        List<UUID> ids = sessions.stream().map(PracticeSession::getQuestionId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return questionRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(PracticeQuestion::getId, PracticeQuestion::getText));
    }

    private String topicOf(UUID questionId) {
        return questionRepository.findById(questionId).map(PracticeQuestion::getTopic).orElse(null);
    }

    private PracticeSessionResponse toSessionResponse(PracticeSession session,
                                                      PracticeQuestion question,
                                                      StarFeedback feedback) {
        return new PracticeSessionResponse(
                session.getId(), session.getQuestionId(),
                question == null ? null : question.getText(),
                question == null ? null : question.getTopic(),
                session.getAnswer(), feedback != null, feedback,
                session.getRubricVersion(), session.getProvider(), session.getRating(),
                session.getCreatedAt());
    }

    private String write(StarFeedback feedback) {
        try {
            return objectMapper.writeValueAsString(feedback);
        } catch (Exception e) {
            return null;
        }
    }

    private StarFeedback read(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, StarFeedback.class);
        } catch (Exception e) {
            return null;
        }
    }
}
