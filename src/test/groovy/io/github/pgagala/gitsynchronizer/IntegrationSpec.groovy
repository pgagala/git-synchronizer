package io.github.pgagala.gitsynchronizer

import io.github.pgagala.gitsynchronizer.processexecutor.ProcessExecutor
import io.github.pgagala.gitsynchronizer.processexecutor.Response
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.wait.strategy.Wait
import spock.lang.Specification

import java.time.Duration

abstract class IntegrationSpec extends Specification {

    private static final DockerComposeContainer dockerComposeContainer
    protected static final String gitServerIp
    protected static final String gitServerNetwork

    static {
        startGitServerImageOrThrowException()
        Docker.startGitUserImageOrThrowException()
        dockerComposeContainer = new DockerComposeContainer(new File("gitserver/docker-compose.yaml"))
                .withBuild(true)
                .withServices("git-server")
                .withExposedService("git-server", 80)
                .withRemoveImages(DockerComposeContainer.RemoveImages.ALL)
                .waitingFor("git-server", Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)))
                .withPull(false)
                .withBuild(false)
        dockerComposeContainer.start()
        def network = dockerComposeContainer.getContainerByServiceName("git-server_1").get()
                .containerInfo.getNetworkSettings()
                .getNetworks().find { it.key.contains("my-net") }
        gitServerIp = network.value.ipAddress
        gitServerNetwork = network.key
    }

    private static void startGitServerImageOrThrowException() throws InterruptedException {
        Response response = startGitServer()
        if(response.isFailure()) {
            throw new IllegalStateException("Cannot start docker git image: " + response.toString())
        }
    }

    private static Response startGitServer() throws InterruptedException {
       return new ProcessExecutor(new File("./")).execute(["docker", "load", "-i", "./docker_images/git-image_server_latest.tar.gz"], "loading git-server image")
    }
}
