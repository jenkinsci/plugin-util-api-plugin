package io.jenkins.plugins.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.hm.hafner.util.FilteredLog;

/**
 * Evaluates a given set of quality gates.
 *
 * @param <T>
 *         the concrete type of the quality gates
 *
 * @author Johannes Walter
 */
public abstract class QualityGateEvaluator<T extends QualityGate> {
    private final List<T> qualityGates = new ArrayList<>();

    /**
     * Creates a new quality evaluator for the specified quality gates.
     *
     * @param qualityGates
     *         the quality gates to evaluate
     */
    protected QualityGateEvaluator(final Collection<? extends T> qualityGates) {
        this.qualityGates.addAll(qualityGates);
    }

    /**
     * Enforces the quality gates for the specified run.
     *
     * @param resultHandler
     *         the result handler to publish the result
     * @param log
     *         the logger
     *
     * @return result of the evaluation, expressed by a build state
     */
    public QualityGateResult evaluate(final ResultHandler resultHandler, final FilteredLog log) {
        var result = new QualityGateResult();

        if (qualityGates.isEmpty()) {
            log.logInfo("No quality gates have been set - skipping");
        }
        else {
            log.logInfo("Evaluating quality gates");
            for (T qualityGate : qualityGates) {
                evaluate(qualityGate, result);
            }

            if (result.isSuccessful()) {
                log.logInfo("-> All quality gates have been passed");
            }
            else {
                var message = String.format("-> Some quality gates have been missed: overall result is %s",
                        result.getOverallStatus().getResult());
                log.logInfo(message);
                resultHandler.publishResult(result.getOverallStatus(), message);
            }
            log.logInfo("-> Details for each quality gate:");
            result.getMessages().forEach(message -> log.logInfo("   - " + message));
        }

        return result;
    }

    protected abstract void evaluate(T qualityGate, QualityGateResult result);

    /**
     * Appends all the specified quality gates to the end of the existing quality gates.
     *
     * @param additionalQualityGates
     *         the quality gates to add
     */
    public void addAll(final Collection<? extends T> additionalQualityGates) {
        this.qualityGates.addAll(additionalQualityGates);
    }

    /**
     * Returns whether at least one quality gate has been added.
     *
     * @return {@code true} if at least one quality gate has been added, {@code false} otherwise
     */
    public boolean isEnabled() {
        return !qualityGates.isEmpty();
    }
}
