package io.jenkins.plugins.util;

import hudson.model.Result;
import hudson.model.Run;

/**
 * A {@link ResultHandler} that sets the overall build result of the {@link Run}.
 *
 * @author Devin Nusbaum
 */
@SuppressWarnings("deprecation")
public class RunResultHandler implements StageResultHandler, ResultHandler {
    private final Run<?, ?> run;

    /**
     * Creates a new instance of {@link RunResultHandler}.
     *
     * @param run
     *         the run to set the result for
     */
    public RunResultHandler(final Run<?, ?> run) {
        this.run = run;
    }

    @Override
    public void setResult(final Result result, final String message) {
        publishResult(result, message);
    }

    @Override
    public void publishResult(final Result result, final String message) {
        if (result.equals(Result.UNSTABLE) || result.equals(Result.FAILURE)) {
            run.setResult(result);
        }
    }

    @Override
    public void publishResult(final QualityGateStatus status, final String message) {
        setResult(status.getResult(), message);
    }
}
