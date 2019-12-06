package io.jenkins.plugins.util;

import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchRule;

import jenkins.model.Jenkins;

import static com.tngtech.archunit.base.DescribedPredicate.*;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.*;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.*;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * Defines several architecture rules that should be enforced for every Jenkins plugin.
 *
 * @author Ullrich Hafner
 */
public final class PluginArchitectureRules {
    /**
     * Direct calls to {@link Jenkins#getInstance()} or {@link Jenkins#getInstanceOrNull()}} are prohibited since these
     * methods require a running Jenkins instance. Otherwise the accessor of this method cannot be unit tested. Create a
     * new {@link JenkinsFacade} object to access the running Jenkins instance. If your required method is missing you
     * need to add it to {@link JenkinsFacade}.
     */
    public static final ArchRule NO_JENKINS_INSTANCE_CALL =
            noClasses().that().doNotHaveSimpleName("JenkinsFacade")
                    .should().callMethod(Jenkins.class, "getInstance")
                    .orShould().callMethod(Jenkins.class, "getInstanceOrNull")
                    .orShould().callMethod(Jenkins.class, "getActiveInstance")
                    .orShould().callMethod(Jenkins.class, "get");

    /** Junit 5 test classes should not be public. */
    public static final ArchRule NO_PUBLIC_TEST_CLASSES =
            noClasses().that().haveSimpleNameEndingWith("Test")
                    .and().doNotHaveModifier(JavaModifier.ABSTRACT)
                    .and(doNot(have(simpleNameEndingWith("ITest"))))
                    .should().bePublic();

    /** Some packages that are transitive dependencies of Jenkins should not be used at all. */
    public static final ArchRule NO_FORBIDDEN_PACKAGE_ACCESSED
            = noClasses().should().dependOnClassesThat(resideInAnyPackage(
            "org.apache.commons.lang..",
            "org.joda.time..",
            "javax.xml.bind..",
            "net.jcip.annotations..",
            "javax.annotation..",
            "junit..",
            "org.hamcrest..",
            "com.google.common.."));

    private PluginArchitectureRules() {
        // prevents instantiation
    }
}
