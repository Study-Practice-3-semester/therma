package com.evraz.therma.engine;

import com.evraz.therma.dto.Priority;
import com.evraz.therma.dto.RuleCode;

/**
 * Модель экспертного правила.
 * Каждое правило имеет код, приоритет, условие срабатывания и действие.
 */
public class Rule {
    private RuleCode code;
    private Priority priority;
    private String description;

    public Rule(RuleCode code, Priority priority, String description) {
        this.code = code;
        this.priority = priority;
        this.description = description;
    }

    public RuleCode getCode() { return code; }
    public Priority getPriority() { return priority; }
    public String getDescription() { return description; }
}
