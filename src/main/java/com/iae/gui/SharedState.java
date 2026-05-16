package com.iae.gui;

import com.iae.domain.EvaluationResult;
import java.util.List;

public final class SharedState {
    public static volatile List<EvaluationResult> pendingResults;
    private SharedState() {}
}
