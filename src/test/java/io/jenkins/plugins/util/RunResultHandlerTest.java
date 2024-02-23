package io.jenkins.plugins.util;

import org.junit.jupiter.api.Test;

import hudson.model.Result;
import hudson.model.Run;

import static org.mockito.Mockito.*;

class RunResultHandlerTest {
    private static final String MESSAGE = "message";

    @Test
    void shouldSetRunResult() {
        var run = mock(Run.class);

        var handler = new RunResultHandler(run);
        handler.publishResult(QualityGateStatus.PASSED, MESSAGE);

        verifyNoInteractions(run);

        handler.publishResult(QualityGateStatus.WARNING, MESSAGE);
        verify(run).setResult(Result.UNSTABLE);

        handler.publishResult(QualityGateStatus.FAILED, MESSAGE);
        verify(run).setResult(Result.FAILURE);
    }
}
