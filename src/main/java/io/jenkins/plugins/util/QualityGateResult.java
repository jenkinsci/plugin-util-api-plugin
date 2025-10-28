package io.jenkins.plugins.util;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import hudson.model.Result;

/**
 * Result of a quality gate evaluation. Aggregates the individual results of the quality gates into an overall status.
 *
 * @author Ullrich Hafner
 */
public class QualityGateResult implements Serializable {
    @Serial
    private static final long serialVersionUID = 1626549055698872334L;

    private QualityGateStatus overallStatus;
    @SuppressWarnings("PMD.LooseCoupling")
    private final ArrayList<QualityGateResultItem> items = new ArrayList<>();

    /**
     * Creates a new instance of {@link QualityGateResult} that has its overall status set to
     * {@link QualityGateStatus#INACTIVE}.
     */
    public QualityGateResult() {
        this(QualityGateStatus.INACTIVE);
    }

    /**
     * Creates a new instance of {@link QualityGateResult} with the specified overall status.
     *
     * @param overallStatus
     *         the overall status of all quality gates
     */
    public QualityGateResult(final QualityGateStatus overallStatus) {
        this.overallStatus = overallStatus;
    }

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

    @Whitelisted
    public QualityGateStatus getOverallStatus() {
        return overallStatus;
    }

    @Whitelisted
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
        return "[%s]: ≪%s≫ - (Actual value: %s, Quality gate: %.2f)".formatted(
                item.getQualityGate().getName(),
                item.getStatus().getDescription(),
                item.getActualValue(),
                item.getQualityGate().getThreshold());
    }

    @Override
    public String toString() {
        return getOverallStatus().toString();
    }

    /**
     * Represents a single item of the quality gate results.
     */
    public static class QualityGateResultItem implements Serializable {
        @Serial
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

    /**
     * Remote API to list the overview of the quality gate evaluation.
     */
    @ExportedBean
    public static class QualityGateResultApi {
        private final QualityGateResult qualityGateResult;

        /**
         * Creates a new instance of {@link QualityGateResultApi}.
         *
         * @param qualityGateResult
         *         the quality gate result to show
         */
        public QualityGateResultApi(final QualityGateResult qualityGateResult) {
            this.qualityGateResult = qualityGateResult;
        }

        @Exported(inline = true)
        public QualityGateStatus getOverallResult() {
            return qualityGateResult.getOverallStatus();
        }

        @Exported(inline = true)
        public Collection<QualityGateItemApi> getResultItems() {
            return qualityGateResult.getResultItems()
                    .stream()
                    .map(QualityGateItemApi::new)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Remote API to show the content of an individual quality gate item.
     */
    @ExportedBean
    public static class QualityGateItemApi {
        private final QualityGateResultItem item;

        /**
         * Creates a new instance of {@link QualityGateItemApi}.
         *
         * @param item
         *         the quality gate result item to show
         */
        public QualityGateItemApi(final QualityGateResultItem item) {
            this.item = item;
        }

        @Exported
        public String getQualityGate() {
            return item.getQualityGate().getName();
        }

        @Exported
        public double getThreshold() {
            return item.getQualityGate().getThreshold();
        }

        @Exported(inline = true)
        public Result getResult() {
            return item.getStatus().getResult();
        }

        @Exported
        public String getValue() {
            return item.getActualValue();
        }
    }
}
