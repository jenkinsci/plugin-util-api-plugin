package io.jenkins.plugins.util;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.collection.IsCollectionWithSize.*;
import static org.hamcrest.collection.IsEmptyCollection.*;

/**
 * Verifies the new rule with method scope.
 */
class JenkinsRuleResolverWithMethodScopeTest {
    @EnableJenkins
    @Test
    void jenkinsRuleIsAccessible(final JenkinsRule rule) throws IOException {
        assertThat(rule.jenkins.getJobNames(), empty());
        rule.createFreeStyleProject("job-0");
        assertThat(rule.jenkins.getJobNames(), hasSize(1));
    }
}
