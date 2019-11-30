package io.jenkins.plugins.util;

import java.io.IOException;
import java.util.Optional;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import hudson.model.Action;
import hudson.model.Job;

/**
 * A job action displays a link on the side panel of a job that refers to the last build that contains results (i.e. a
 * {@link BuildAction} with a corresponding result). This action also is responsible to render the historical trend via
 * its associated 'floatingBox.jelly' view.
 *
 * @param <T>
 *         type of the results
 *
 * @author Ullrich Hafner
 */
public abstract class JobAction<T extends BuildAction<?>> implements Action {
    private final Job<?, ?> owner;
    private final Class<T> buildActionClass;

    /**
     * Creates a new instance of {@link JobAction}.
     *
     * @param owner
     *         the job that owns this action
     * @param buildActionClass
     *         the type of the action to find
     */
    public JobAction(final Job<?, ?> owner, final Class<T> buildActionClass) {
        this.owner = owner;
        this.buildActionClass = buildActionClass;
    }

    /**
     * Returns the job this action belongs to.
     *
     * @return the job
     */
    public Job<?, ?> getOwner() {
        return owner;
    }

    protected Class<T> getBuildActionClass() {
        return buildActionClass;
    }

    /**
     * Redirects the index page to the last result.
     *
     * @param request
     *         Stapler request
     * @param response
     *         Stapler response
     *
     * @throws IOException
     *         in case of an error
     */
    @SuppressWarnings("unused") // Called by jelly view
    public void doIndex(final StaplerRequest request, final StaplerResponse response) throws IOException {
        Optional<T> action = getLatestAction();
        if (action.isPresent()) {
            T buildAction = action.get();
            response.sendRedirect2(String.format("../../../%s%s",
                    buildAction.getOwner().getUrl(), buildAction.getUrlName()));
        }
    }

    /**
     * Returns the latest results for this job.
     *
     * @return the latest results (if available)
     */
    public Optional<T> getLatestAction() {
        return BuildAction.getBuildActionFromHistoryStartingFrom(owner.getLastBuild(), buildActionClass);
    }
}
