package io.jenkins.plugins.util;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.AbstractItem;
import hudson.model.BallColor;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Run;
import hudson.security.Permission;
import jenkins.model.Jenkins;

/**
 * Facade to Jenkins server. Encapsulates all calls to the running Jenkins server so that tests can replace this facade
 * with a stub.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings("PMD.ExcessiveImports")
public class JenkinsFacade implements Serializable {
    private static final long serialVersionUID = 1904631270145841113L;

    /**
     * Returns the discovered instances for the given extension type.
     *
     * @param extensionType
     *         The base type that represents the extension point. Normally {@link ExtensionPoint} subtype but that's not
     *         a hard requirement.
     * @param <T>
     *         type of the extension
     *
     * @return the discovered instances, might be an empty list
     */
    public <T> List<T> getExtensionsFor(final Class<T> extensionType) {
        return getJenkins().getExtensionList(extensionType);
    }

    /**
     * Returns the discovered instances for the given descriptor type.
     *
     * @param <T>
     *         type of the describable
     * @param <D>
     *         type of the descriptor
     * @param describableType
     *         the base type that represents the descriptor of the describable
     *
     * @return the discovered instances, might be an empty list
     */
    public <T extends Describable<T>, D extends Descriptor<T>> DescriptorExtensionList<T, D> getDescriptorsFor(
            final Class<T> describableType) {
        return getJenkins().getDescriptorList(describableType);
    }

    /**
     * Checks if the current security principal has this permission.
     *
     * @param permission
     *         the permission to check for
     *
     * @return {@code false} if the user doesn't have the permission
     */
    public boolean hasPermission(final Permission permission) {
        return getJenkins().getACL().hasPermission(permission);
    }

    /**
     * Gets a {@link Job} by its full name. Full names are like path names, where each name of {@link Item} is combined
     * by '/'.
     *
     * @param name
     *         the full name of the job
     *
     * @return the selected job, if it exists under the given full name and if it is accessible
     */
    @SuppressWarnings("unchecked")
    public Optional<Job<?, ?>> getJob(final String name) {
        try {
            return Optional.ofNullable(getJenkins().getItemByFullName(name, Job.class));
        }
        catch (RuntimeException x) { // TODO switch to simple catch (AccessDeniedException) when baseline includes Spring Security
            if (x.getClass().getSimpleName().startsWith("AccessDeniedException")) {
                return Optional.empty();
            } else {
                throw x;
            }
        }
    }

    /**
     * Gets a {@link Run build} by the full ID.
     *
     * @param id
     *         the ID of the build
     *
     * @return the selected build, if it exists with the given ID and if it is accessible
     */
    public Optional<Run<?, ?>> getBuild(final String id) {
        try {
            return Optional.ofNullable(Run.fromExternalizableId(id));
        }
        catch (RuntimeException x) { // TODO switch to simple catch (AccessDeniedException) when baseline includes Spring Security
            if (x.getClass().getSimpleName().startsWith("AccessDeniedException")) {
                return Optional.empty();
            } else {
                throw x;
            }
        }
    }

    /**
     * Returns the absolute URL for the specified ball icon.
     *
     * @param color
     *         the color
     *
     * @return the absolute URL
     */
    public String getImagePath(final BallColor color) {
        return getContextPath() + "/images/16x16/" + color.getImage();
    }

    /**
     * Returns the absolute URL for the specified icon.
     *
     * @param icon
     *         the icon URL
     *
     * @return the absolute URL
     */
    public String getImagePath(final String icon) {
        return getContextPath() + Jenkins.RESOURCE_PATH + icon;
    }

    private String getContextPath() {
        StaplerRequest currentRequest = Stapler.getCurrentRequest();
        if (currentRequest != null) {
            return currentRequest.getContextPath();
        }
        return StringUtils.EMPTY;
    }

    /**
     * Returns an absolute URL for the specified url elements: e.g., creates the sequence ${rootUrl}/element1/element2.
     *
     * @param urlElements
     *         the url elements
     *
     * @return the absolute URL
     */
    public String getAbsoluteUrl(final String... urlElements) {
        return getAbsoluteUrl(StringUtils.join(urlElements, "/"));

    }

    private String getAbsoluteUrl(final String url) {
        try {
            String rootUrl = getJenkins().getRootUrl();
            if (rootUrl != null) {
                return rootUrl + url;
            }
        }
        catch (IllegalStateException ignored) {
            // ignored
        }
        return url;
    }

    /**
     * Returns the full names of all available jobs. The full name is given by {@link AbstractItem#getFullName()}.
     *
     * @return the full names of all jobs
     */
    public Set<String> getAllJobNames() {
        return getAllJobs().stream().map(this::getFullNameOf).collect(Collectors.toSet());
    }

    /**
     * Returns all available jobs.
     *
     * @return all jobs
     */
    @SuppressWarnings("rawtypes")
    public List<Job> getAllJobs() {
        return getJenkins().getAllItems(Job.class);
    }

    /**
     * Returns the full name of the specified job.
     *
     * @param job
     *         the job to get the name for
     *
     * @return the full name
     */
    public String getFullNameOf(final Job<?, ?> job) {
        return job.getFullName(); // getFullName is final
    }

    /**
     * Returns whether the plugin with the specified ID (short name, artifact ID) is installed.
     *
     * @param pluginId
     *         the ID of the plugin
     *
     * @return {@code true} if the plugin is installed, {@code false} if not
     */
    public boolean isPluginInstalled(final String pluginId) {
        return getJenkins().getPlugin(pluginId) != null;
    }

    private Jenkins getJenkins() {
        return Jenkins.get();
    }
}
