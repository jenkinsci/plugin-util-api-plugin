package io.jenkins.plugins.util;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.Issue;

import org.jenkinsci.plugins.workflow.actions.WarningAction;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import hudson.model.Action;
import hudson.model.Result;
import hudson.model.Run;

import static org.mockito.Mockito.*;

class PipelineResultHandlerTest {
    private static final String MESSAGE = "message";

    @Test
    void shouldSetRunResult() {
        var run = mock(Run.class);
        var flowNode = mock(FlowNode.class);

        var handler = new PipelineResultHandler(run, flowNode);
        handler.publishResult(QualityGateStatus.PASSED, MESSAGE);

        verifyNoInteractions(run);
        verifyNoInteractions(flowNode);

        handler.publishResult(QualityGateStatus.WARNING, MESSAGE);
        verify(run).setResult(Result.UNSTABLE);
        verify(flowNode).addOrReplaceAction(argThat(action -> hasFlowNode(action, Result.UNSTABLE)));

        handler.publishResult(QualityGateStatus.FAILED, MESSAGE);
        verify(run).setResult(Result.FAILURE);
        verify(flowNode).addOrReplaceAction(argThat(action -> hasFlowNode(action, Result.FAILURE)));
    }

    @Test
    void shouldReplaceRunResult() {
        var run = mock(Run.class);
        var flowNode = mock(FlowNode.class);

        var handler = new PipelineResultHandler(run, flowNode);
        when(flowNode.getPersistentAction(WarningAction.class)).thenReturn(new WarningAction(Result.UNSTABLE));

        handler.publishResult(QualityGateStatus.WARNING, MESSAGE);
        verify(flowNode, never()).addOrReplaceAction(any());

        handler.publishResult(QualityGateStatus.ERROR, MESSAGE);
        verify(flowNode).addOrReplaceAction(argThat(action -> hasFlowNode(action, Result.FAILURE)));
    }

    @Test @Issue("JENKINS-72059")
    void shouldSetStageResult() {
        var run = mock(Run.class);
        var flowNode = mock(FlowNode.class);

        var handler = new PipelineResultHandler(run, flowNode);
        handler.publishResult(QualityGateStatus.PASSED, MESSAGE);

        verifyNoInteractions(run);
        verifyNoInteractions(flowNode);

        handler.publishResult(QualityGateStatus.NOTE, MESSAGE);
        verifyNoInteractions(run);
        verify(flowNode).addOrReplaceAction(argThat(action -> hasFlowNode(action, Result.UNSTABLE)));

        handler.publishResult(QualityGateStatus.ERROR, MESSAGE);
        verifyNoInteractions(run);
        verify(flowNode).addOrReplaceAction(argThat(action -> hasFlowNode(action, Result.FAILURE)));
    }

    private boolean hasFlowNode(final Action action, final Result result) {
        if (!(action instanceof WarningAction)) {
            return false;
        }
        WarningAction warningAction = (WarningAction) action;
        return result.equals(warningAction.getResult()) && MESSAGE.equals(warningAction.getMessage());
    }
}
