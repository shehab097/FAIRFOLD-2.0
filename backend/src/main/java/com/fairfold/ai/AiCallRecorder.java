package com.fairfold.ai;

import com.fairfold.config.AppProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Writes {@code ai_calls} in its own transaction so blocked or failed calls are
 * recorded even when the surrounding screening item rolls back.
 */
@Service
public class AiCallRecorder {

    private final AiCallRepository repository;
    private final AppProperties.Ai config;

    public AiCallRecorder(AiCallRepository repository, AppProperties properties) {
        this.repository = repository;
        this.config = properties.ai();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID userId, UUID applicationId, String purpose, String model,
                       int promptTokens, int completionTokens, String status) {
        AiCall call = new AiCall();
        call.setPurpose(purpose);
        call.setUserId(userId);
        call.setApplicationId(applicationId);
        call.setModel(model);
        call.setPromptTokens(Math.max(0, promptTokens));
        call.setCompletionTokens(Math.max(0, completionTokens));
        call.setCost(cost(promptTokens, completionTokens));
        call.setStatus(status);
        repository.save(call);
    }

    private BigDecimal cost(int promptTokens, int completionTokens) {
        BigDecimal input = config.inputPrice()
                .multiply(BigDecimal.valueOf(Math.max(0, promptTokens)));
        BigDecimal output = config.outputPrice()
                .multiply(BigDecimal.valueOf(Math.max(0, completionTokens)));
        return input.add(output)
                .divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP);
    }
}
