package com.evraz.therma.dto;

import java.time.Instant;
import java.util.List;

public class Recommendation {
    private Long id;
    private String cowperId;
    private Instant createdAt;
    private String ruleCode;
    private String message;
    private Priority priority;
    private RecommendationStatus status;
    private ExpectedEffect expectedEffect;
    private List<String> reasoning;
    private String targetParameter;
    private Double suggestedValueMin;
    private Double suggestedValueMax;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCowperId() { return cowperId; }
    public void setCowperId(String cowperId) { this.cowperId = cowperId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public RecommendationStatus getStatus() { return status; }
    public void setStatus(RecommendationStatus status) { this.status = status; }

    public ExpectedEffect getExpectedEffect() { return expectedEffect; }
    public void setExpectedEffect(ExpectedEffect expectedEffect) { this.expectedEffect = expectedEffect; }

    public List<String> getReasoning() { return reasoning; }
    public void setReasoning(List<String> reasoning) { this.reasoning = reasoning; }

    public String getTargetParameter() { return targetParameter; }
    public void setTargetParameter(String targetParameter) { this.targetParameter = targetParameter; }

    public Double getSuggestedValueMin() { return suggestedValueMin; }
    public void setSuggestedValueMin(Double suggestedValueMin) { this.suggestedValueMin = suggestedValueMin; }

    public Double getSuggestedValueMax() { return suggestedValueMax; }
    public void setSuggestedValueMax(Double suggestedValueMax) { this.suggestedValueMax = suggestedValueMax; }
}
