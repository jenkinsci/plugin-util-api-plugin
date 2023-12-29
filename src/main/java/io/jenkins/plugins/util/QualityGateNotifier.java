package io.jenkins.plugins.util;

/**
 * Notifies the build or stage about the quality gate result.
 *
 *  @author Ullrich Hafner
 */
public interface QualityGateNotifier {
    /**
     * Called to notify the build or stage about the quality gate status.
     *
     * @param status
     *         the quality gate status
     * @param message
     *         a message that describes the cause for the result
     */
    void publishResult(QualityGateStatus status, String message);
}
