package io.jenkins.plugins.util;

import java.util.List;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.util.FilteredLog;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class QualityGateEvaluatorTest {
    private static final String FAILURE_MESSAGE = "-> Some quality gates have been missed: overall result is FAILURE";

    @Test
    void shouldDoNothing() {
        var log = new FilteredLog();
        var result = createEvaluator(log);

        assertThat(result.getOverallStatus()).isEqualTo(QualityGateStatus.INACTIVE);
        assertThat(log.getInfoMessages())
                .contains("No quality gates have been set - skipping");
    }

    private QualityGateResult createEvaluator(final FilteredLog log) {
        var evaluator = spy(QualityGateEvaluator.class);

        return evaluator.evaluate(mock(ResultHandler.class), log);
    }

    @Test
    void shouldCreateSuccessfulResult() {
        var log = new FilteredLog();

        var evaluator = spy(QualityGateEvaluator.class);
        addQualityGate(evaluator);

        var resultHandler = mock(ResultHandler.class);
        var result = evaluator.evaluate(resultHandler, log);

        assertThat(result.getOverallStatus()).isEqualTo(QualityGateStatus.INACTIVE);
        assertThat(log.getInfoMessages())
                .contains("Evaluating quality gates",
                        "-> All quality gates have been passed");
        verifyNoInteractions(resultHandler);
    }

    @Test
    void shouldCreateFailedResult() {
        var log = new FilteredLog();

        var evaluator = spy(QualityGateEvaluator.class);

        var qualityGateResult = mock(QualityGateResult.class);
        when(qualityGateResult.isSuccessful()).thenReturn(false);
        when(qualityGateResult.getOverallStatus()).thenReturn(QualityGateStatus.ERROR);
        when(evaluator.createResult()).thenReturn(qualityGateResult);

        addQualityGate(evaluator);

        var resultHandler = mock(ResultHandler.class);
        var result = evaluator.evaluate(resultHandler, log);

        assertThat(result.getOverallStatus()).isEqualTo(QualityGateStatus.ERROR);
        assertThat(log.getInfoMessages())
                .contains("Evaluating quality gates", FAILURE_MESSAGE);
        verify(resultHandler).publishResult(QualityGateStatus.ERROR, FAILURE_MESSAGE);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void addQualityGate(final QualityGateEvaluator evaluator) {
        var qualityGate = mock(QualityGate.class);
        evaluator.addAll(List.of(qualityGate));
    }
}
