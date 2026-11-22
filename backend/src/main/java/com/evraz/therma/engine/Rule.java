package com.evraz.therma.engine;

import com.evraz.therma.dto.Priority;
import com.evraz.therma.dto.RuleCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Rule {
    private final RuleCode code;
    private final Priority priority;
    private final String description;
}
