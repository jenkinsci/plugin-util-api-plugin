package io.jenkins.plugins.util;

import hudson.model.Result;

/**
 * A {@link ResultHandler} that does nothing.
 *
 * @author Ullrich Hafner
 */
public class NullResultHandler implements ResultHandler {
    @Override
    public void publishResult(final QualityGateStatus status, final String message) {
        // empty
    }

    @Override
    public void publishResult(final Result result, final String message) {
        // empty
    }
}
