package io.jenkins.plugins.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.util.Objects;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;

/**
 * Tests the docker agent support of {@link IntegrationTest}: starts a Jenkins controller together with an agent that
 * runs in a docker container and verifies that the agent is connected.
 *
 * @author Ullrich Hafner
 */
class DockerAgentTest extends IntegrationTestWithJenkinsPerSuite {
    private static AgentContainer agentContainer;

    @BeforeAll
    static void startAgentContainer() {
        assumeThat(DockerClientFactory.instance().isDockerAvailable())
                .as("Docker is not available, skipping test")
                .isTrue();

        agentContainer = new AgentContainer();
        agentContainer.start();
    }

    @AfterAll
    static void stopAgentContainer() {
        if (agentContainer != null) {
            agentContainer.stop();
        }
    }

    @Test
    void shouldStartDockerAgentAndConnectToController() {
        var agent = createDockerAgent(agentContainer);

        assertThat(agent.getNodeName()).isEqualTo(DOCKER_AGENT_NAME);

        var computer = Objects.requireNonNull(agent.toComputer());
        assertThat(computer.isOnline()).isTrue();
        assertThat(computer.isOffline()).isFalse();
    }

    @Test
    void shouldAssignLabelToDockerAgent() {
        var label = "docker-label";
        var agent = createDockerAgent(agentContainer, label);

        assertThat(agent.getLabelString()).isEqualTo(label);
        assertThat(Objects.requireNonNull(agent.toComputer()).isOnline()).isTrue();
    }
}
