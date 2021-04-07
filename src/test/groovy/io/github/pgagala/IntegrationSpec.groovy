package io.github.pgagala

import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.wait.strategy.Wait
import spock.lang.Specification

import java.time.Duration

abstract class IntegrationSpec extends Specification {

    private static final DockerComposeContainer dockerComposeContainer
    protected static final String gitServerIp
    protected static final String gitServerNetwork

    static {
        dockerComposeContainer = new DockerComposeContainer(new File("gitserver/docker-compose.yaml"))
                .withBuild(true)
                .withServices("git-server")
                .withExposedService("git-server", 80)
                .withRemoveImages(DockerComposeContainer.RemoveImages.ALL)
                .waitingFor("git-server", Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)))
                .withPull(true)
                .withBuild(true)
        dockerComposeContainer.start()
        def network = dockerComposeContainer.getContainerByServiceName("git-server_1").get().containerInfo.getNetworkSettings().getNetworks().find { it.key.contains("my-net") }
        gitServerIp = network.value.ipAddress
        gitServerNetwork = network.key
    }

}
