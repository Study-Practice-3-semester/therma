package com.evraz.therma.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
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
}
