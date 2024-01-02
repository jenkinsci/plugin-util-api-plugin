package io.jenkins.plugins.util;

import java.io.Serializable;

import edu.hm.hafner.util.VisibleForTesting;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.verb.POST;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.BuildableItem;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

/**
 * Defines a quality gate based on a specific threshold of code coverage in the current build. After a build has been
 * finished, a set of {@link QualityGate quality gates} will be evaluated and the overall quality gate status will be
 * reported in Jenkins UI.
 *
 * @author Johannes Walter
 */
public abstract class QualityGate extends AbstractDescribableImpl<QualityGate> implements Serializable {
    private static final long serialVersionUID = -397278599489426668L;

    private double threshold;
    private QualityGateCriticality criticality = QualityGateCriticality.UNSTABLE;

    /**
     * Creates a new instance of {@link QualityGate}.
     *
     * @param threshold
     *         minimum or maximum value that triggers this quality gate
     */
    protected QualityGate(final double threshold) {
        super();

        this.threshold = threshold;
    }

    /**
     * Sets the criticality of this quality gate. When a quality gate has been missed, this property determines whether
     * the result of the associated coverage stage will be marked as unstable or failure.
     *
     * @param criticality
     *         the criticality for this quality gate
     */
    @DataBoundSetter
    public final void setCriticality(final QualityGateCriticality criticality) {
        this.criticality = criticality;
    }

    /**
     * Returns a human-readable name of the quality gate.
     *
     * @return a human-readable name
     */
    public abstract String getName();

    protected void setThreshold(final double threshold) {
        this.threshold = threshold;
    }

    @Override
    public String toString() {
        return getName() + String.format(" - %s: %f", getCriticality(), getThreshold());
    }

    public final double getThreshold() {
        return threshold;
    }

    public final QualityGateCriticality getCriticality() {
        return criticality;
    }

    public final QualityGateStatus getStatus() {
        return getCriticality().getStatus();
    }

    /**
     * Determines the Jenkins build result if the quality gate is failed.
     */
    public enum QualityGateCriticality {
        /** The stage will be marked with a warning. */
        NOTE(QualityGateStatus.NOTE),

        /** The stage and the build will be marked as unstable. */
        UNSTABLE(QualityGateStatus.WARNING),

        /** The stage will be marked as failed. */
        ERROR(QualityGateStatus.ERROR),

        /** The stage and the build will be marked as failed. */
        FAILURE(QualityGateStatus.FAILED);

        private final QualityGateStatus status;

        QualityGateCriticality(final QualityGateStatus status) {
            this.status = status;
        }

        /**
         * Returns the status.
         *
         * @return the status
         */
        public QualityGateStatus getStatus() {
            return status;
        }
    }

    /**
     * Descriptor of the {@link QualityGate}.
     */
    @Extension
    public static class QualityGateDescriptor extends Descriptor<QualityGate> {
        private final JenkinsFacade jenkins;

        @VisibleForTesting
        QualityGateDescriptor(final JenkinsFacade jenkinsFacade) {
            super();

            jenkins = jenkinsFacade;
        }

        /**
         * Creates a new descriptor.
         */
        @SuppressWarnings("unused") // Required for Jenkins Extensions
        public QualityGateDescriptor() {
            this(new JenkinsFacade());
        }

        /**
         * Returns a model with all {@link QualityGateCriticality criticalities} that can be used in quality gates.
         *
         * @param project
         *         the project that is configured
         *
         * @return a model with all {@link QualityGateCriticality criticalities}.
         */
        @POST
        @SuppressWarnings("unused") // used by Stapler view data binding
        public ListBoxModel doFillCriticalityItems(@AncestorInPath final BuildableItem project) {
            if (jenkins.hasPermission(Jenkins.READ)) {
                ListBoxModel options = new ListBoxModel();
                if (project instanceof FreeStyleProject) {
                    options.add(Messages.QualityGate_Unstable(), QualityGateCriticality.UNSTABLE.name());
                    options.add(Messages.QualityGate_Failure(), QualityGateCriticality.FAILURE.name());
                }
                else {
                    options.add(Messages.QualityGate_UnstableStage(), QualityGateCriticality.NOTE.name());
                    options.add(Messages.QualityGate_UnstableRun(), QualityGateCriticality.UNSTABLE.name());
                    options.add(Messages.QualityGate_FailureStage(), QualityGateCriticality.ERROR.name());
                    options.add(Messages.QualityGate_FailureRun(), QualityGateCriticality.FAILURE.name());
                }
                return options;
            }
            return new ListBoxModel();
        }
    }
}
