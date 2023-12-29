package io.jenkins.plugins.util;

import hudson.model.Result;

/**
 * Handles the setting of the build or stage result.
 *
 * @author Ullrich Hafner
 */
public interface ResultHandler {
    /**
     * Called to notify the build or stage about the result that is derived from a quality gate status.
     *
     * @param status
     *         the quality gate status
     * @param message
     *         a message that describes the cause for the result
     */
    void publishResult(QualityGateStatus status, String message);

    /**
     * Called to notify the build or stage about the new result.
     *
     * @param result
     *         the result
     * @param message
     *         a message that describes the cause for the result
     */
    void publishResult(Result result, String message);
}
