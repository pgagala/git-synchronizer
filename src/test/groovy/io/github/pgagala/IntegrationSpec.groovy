package io.github.pgagala

import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.wait.strategy.Wait
import spock.lang.Specification

abstract class IntegrationSpec extends Specification {

    private static final DockerComposeContainer dockerComposeContainer

    static {
        dockerComposeContainer = new DockerComposeContainer(new File("gitserver/docker-compose.yaml"))
                .withBuild(true)
                .withServices("git-server", "git")
                .withRemoveImages(DockerComposeContainer.RemoveImages.ALL)
                .waitingFor("git-server", Wait.forListeningPort())
                .waitingFor("git", Wait.forListeningPort())
        dockerComposeContainer.start()
    }

}
