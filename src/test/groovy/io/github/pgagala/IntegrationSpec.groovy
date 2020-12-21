package io.github.pgagala

import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.wait.strategy.Wait
import spock.lang.Specification

import java.time.Duration

abstract class IntegrationSpec extends Specification {

    private static final DockerComposeContainer dockerComposeContainer
    protected static final int gitServicePort

    static {
        dockerComposeContainer = new DockerComposeContainer(new File("gitserver/docker-compose.yaml"))
                .withBuild(true)
                .withServices("git-server", "git")
                .withExposedService("git-server", 80)
                .withRemoveImages(DockerComposeContainer.RemoveImages.ALL)
                .waitingFor("git-server", Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)))
                .waitingFor("git", Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)))
        dockerComposeContainer.start()
        gitServicePort = dockerComposeContainer.getServicePort("git-server_1", 80)
    }

}
