package io.jenkins.plugins.util;

import org.jenkinsci.plugins.workflow.actions.WarningAction;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import hudson.model.Result;
import hudson.model.Run;

/**
 * A {@link QualityGateNotifier} that sets the overall build result of the {@link Run} and annotates the given Pipeline
 * step with a {@link WarningAction}.
 *
 * @author Devin Nusbaum
 */
@SuppressWarnings("deprecation")
public class PipelineResultHandler implements StageResultHandler, QualityGateNotifier {
    private final Run<?, ?> run;
    private final FlowNode flowNode;

    /**
     * Creates a new instance of {@link PipelineResultHandler}.
     *
     * @param run
     *         the run to set the result for
     * @param flowNode
     *         the flow node to add a warning to
     */
    public PipelineResultHandler(final Run<?, ?> run, final FlowNode flowNode) {
        this.run = run;
        this.flowNode = flowNode;
    }

    @Override
    public void setResult(final Result result, final String message) {
        run.setResult(result);

        setStageResult(result, message);
    }

    private void setStageResult(final Result result, final String message) {
        WarningAction existing = flowNode.getPersistentAction(WarningAction.class);
        if (existing == null || existing.getResult().isBetterThan(result)) {
            flowNode.addOrReplaceAction(new WarningAction(result).withMessage(message));
        }
    }

    @Override
    public void publishResult(final QualityGateStatus status, final String message) {
        switch (status) {
            case NOTE:
            case ERROR:
                setStageResult(status.getResult(), message);
                break;
            case WARNING:
            case FAILED:
                setResult(status.getResult(), message);
                break;
            default:
                // ignore and do nothing
        }
    }
}
