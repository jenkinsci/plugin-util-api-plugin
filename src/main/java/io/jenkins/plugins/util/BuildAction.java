package io.jenkins.plugins.util;

import edu.hm.hafner.util.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.Serial;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import hudson.model.Action;
import hudson.model.Run;
import jenkins.model.RunAction2;
import jenkins.tasks.SimpleBuildStep.LastBuildAction;

/**
 * Controls the life cycle of the results in a build. This action persists the results of a build and displays a summary
 * on the build page. The actual visualization of the results is defined in the matching {@code summary.jelly} file.
 * This action also provides access to the detail views: these are rendered using a new view instance.
 *
 * @param <T>
 *         type of the results
 *
 * @author Ullrich Hafner
 */
// FIXME: this action does not work for multiple instantiations
public abstract class BuildAction<T> implements LastBuildAction, RunAction2, Serializable {
    @Serial
    private static final long serialVersionUID = -2074456133028895573L;

    private transient Run<?, ?> owner;
    private transient ReentrantLock lock = new ReentrantLock();

    @CheckForNull
    private transient WeakReference<T> resultReference;

    /**
     * Creates a new instance of {@link BuildAction}.
     *
     * @param owner
     *         the associated build that created the result
     * @param result
     *         the result to persist with this action
     */
    protected BuildAction(final Run<?, ?> owner, final T result) {
        this(owner, result, true);
    }

    /**
     * Creates a new instance of {@link BuildAction}.
     *
     * @param owner
     *         the associated build that created the result
     * @param result
     *         the result to persist with this action
     * @param canSerialize
     *         determines whether the result should be persisted in the build folder
     */
    @SuppressFBWarnings(value = "MC", justification = "getResultXmlPath() is a factory method and overridable by design")
    @SuppressWarnings({"PMD.ConstructorCallsOverridableMethod", "this-escape"})
    @VisibleForTesting
    public BuildAction(final Run<?, ?> owner, final T result, final boolean canSerialize) {
        this.owner = owner;
        this.resultReference = new WeakReference<>(result);

        if (canSerialize) {
            createXmlStream().write(getResultXmlPath(), result);
        }
    }

    /**
     * Creates the XML stream to read the results. This method is invoked by the constructor in this {@link BuildAction}
     * so this instance is not yet fully initialized when this factory method is called. So just return the stream
     * instance without accessing any fields.
     *
     * @return the XML stream
     */
    protected abstract AbstractXmlStream<T> createXmlStream();

    public Run<?, ?> getOwner() {
        return owner;
    }

    @Override
    public void onAttached(final Run<?, ?> r) {
        owner = r;
    }

    @Override
    public void onLoad(final Run<?, ?> r) {
        onAttached(r);
    }

    /**
     * Called after deserialization to improve the memory usage.
     *
     * @return this
     */
    @Serial
    protected Object readResolve() {
        lock = new ReentrantLock();

        return this;
    }

    @Override
    public Collection<? extends Action> getProjectActions() {
        return Set.of(createProjectAction());
    }

    protected abstract JobAction<? extends BuildAction<T>> createProjectAction();

    /**
     * Returns the repository statistics. Since the object requires some amount of memory, it is stored in a {@link
     * WeakReference}. So if the current instance has been destroyed by the garbage collector then a new instance will
     * be automatically created by reading the persisted XML data from Jenkins build folder.
     *
     * @return the statistics
     */
    public T getResult() {
        lock.lock();
        try {
            if (resultReference == null) {
                return readResult();
            }
            var result = this.resultReference.get();
            if (result == null) {
                return readResult();
            }
            return result;
        }
        finally {
            lock.unlock();
        }
    }

    private T readResult() {
        T statistics = createXmlStream().read(getResultXmlPath());
        resultReference = new WeakReference<>(statistics);
        return statistics;
    }

    private Path getResultXmlPath() {
        return owner.getRootDir().toPath().resolve(getBuildResultBaseName());
    }

    protected abstract String getBuildResultBaseName();

    /**
     * Returns a {@link BuildAction} of the specified baseline build. If there is no such action for the baseline then
     * the previous build is inspected, and so on. If no previous build contains a {@link BuildAction} then an empty
     * result is returned.
     *
     * @param baseline
     *         the baseline to start the search with
     * @param buildActionClass
     *         the type of the action to find
     * @param <T>
     *         type of the results
     *
     * @return the next available {@link BuildAction}, or an empty result if there is no such action
     */
    public static <T extends BuildAction<?>> Optional<T> getBuildActionFromHistoryStartingFrom(
            @CheckForNull final Run<?, ?> baseline, final Class<T> buildActionClass) {
        for (Run<?, ?> run = baseline; run != null; run = run.getPreviousBuild()) {
            T action = run.getAction(buildActionClass);
            if (action != null) {
                return Optional.of(action);
            }
        }

        return Optional.empty();
    }
}
