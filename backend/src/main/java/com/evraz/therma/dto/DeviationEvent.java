package com.evraz.therma.dto;

import java.time.Instant;

public class DeviationEvent {
    private String cowperId;
    private Instant timestamp;
    private RuleCode ruleCode;
    private String parameterName;
    private Double actualValue;
    private Double targetValue;
    private Double targetMin;
    private Double targetMax;
    private Priority priority;

    public String getCowperId() { return cowperId; }
    public void setCowperId(String cowperId) { this.cowperId = cowperId; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public RuleCode getRuleCode() { return ruleCode; }
    public void setRuleCode(RuleCode ruleCode) { this.ruleCode = ruleCode; }

    public String getParameterName() { return parameterName; }
    public void setParameterName(String parameterName) { this.parameterName = parameterName; }

    public Double getActualValue() { return actualValue; }
    public void setActualValue(Double actualValue) { this.actualValue = actualValue; }

    public Double getTargetValue() { return targetValue; }
    public void setTargetValue(Double targetValue) { this.targetValue = targetValue; }

    public Double getTargetMin() { return targetMin; }
    public void setTargetMin(Double targetMin) { this.targetMin = targetMin; }

    public Double getTargetMax() { return targetMax; }
    public void setTargetMax(Double targetMax) { this.targetMax = targetMax; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
}
