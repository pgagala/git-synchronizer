package io.github.pgagala

import org.testcontainers.containers.DockerComposeContainer
import spock.lang.Specification


abstract class IntegrationSpec extends Specification {

    private static final DockerComposeContainer dockerComposeContainer
//https://www.testcontainers.org/modules/docker_compose/
    static {
        dockerComposeContainer = new DockerComposeContainer(new File("gitserver/docker-compose.yaml"))
            .withExposedService("git-server", 8080)
        dockerComposeContainer.start()
    }

}
