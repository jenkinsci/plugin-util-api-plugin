package io.jenkins.plugins.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Evaluates a given set of quality gates.
 *
 * @author Johannes Walter
 * @param <T> the concrete type of the quality gates
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
     * @return result of the evaluation, expressed by a build state
     */
    public QualityGateResult evaluate() {
        if (qualityGates.isEmpty()) {
            return new QualityGateResult();
        }

        var result = new QualityGateResult();
        for (T qualityGate : qualityGates) {
            evaluate(qualityGate, result);
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
