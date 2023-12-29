package io.jenkins.plugins.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Result of a quality gate evaluation. Aggregates the individual results of the quality gates into an overall status.
 *
 * @author Ullrich Hafner
 */
public class QualityGateResult implements Serializable {
    private static final long serialVersionUID = -4306601972076922976L;

    private QualityGateStatus overallStatus = QualityGateStatus.INACTIVE;
    private final List<QualityGateResultItem> items = new ArrayList<>();

    /**
     * Adds another quality gate result to the aggregated result.
     *
     * @param qualityGate
     *         the quality gate that has been evaluated
     * @param actualStatus
     *         the status of the quality gate
     * @param actualValue
     *         the value that has been evaluated against the quality gate threshold
     */
    public void add(final QualityGate qualityGate, final QualityGateStatus actualStatus, final String actualValue) {
        items.add(new QualityGateResultItem(actualStatus, qualityGate, actualValue));

        if (actualStatus.isWorseThan(overallStatus)) {
            overallStatus = actualStatus;
        }
    }

    @SuppressWarnings("unused") // Called by jelly view
    public List<QualityGateResultItem> getResultItems() {
        return items;
    }

    public QualityGateStatus getOverallStatus() {
        return overallStatus;
    }

    public boolean isSuccessful() {
        return overallStatus.isSuccessful();
    }

    public boolean isInactive() {
        return overallStatus == QualityGateStatus.INACTIVE;
    }

    public Collection<String> getMessages() {
        return items.stream().map(this::createMessage).collect(Collectors.toList());
    }

    private String createMessage(final QualityGateResultItem item) {
        return String.format("[%s]: ≪%s≫ - (Actual value: %s, Quality gate: %.2f)",
                item.getQualityGate().getName(),
                item.getStatus().getDescription(),
                item.getActualValue(),
                item.getQualityGate().getThreshold());
    }

    /**
     * Represents a single item of the quality gate results.
     */
    public static class QualityGateResultItem implements Serializable {
        private static final long serialVersionUID = -4011767393049355487L;

        private final QualityGateStatus status;
        private final QualityGate qualityGate;
        private final String actualValue;

        QualityGateResultItem(final QualityGateStatus status, final QualityGate qualityGate, final String actualValue) {
            this.status = status;
            this.qualityGate = qualityGate;
            this.actualValue = actualValue;
        }

        public QualityGateStatus getStatus() {
            return status;
        }

        public QualityGate getQualityGate() {
            return qualityGate;
        }

        public String getActualValue() {
            return actualValue;
        }
    }
}

