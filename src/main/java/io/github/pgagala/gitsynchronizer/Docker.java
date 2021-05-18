package io.github.pgagala.gitsynchronizer;

import io.github.pgagala.gitsynchronizer.processexecutor.ProcessExecutor;
import io.github.pgagala.gitsynchronizer.processexecutor.Response;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;

/**
 * Pulling docker image used in application.
 *
 * @author Paweł Gągała
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class Docker {

    static Response pullDockerGitImage() throws InterruptedException {
        return new ProcessExecutor(new File("./"))
            .execute(List.of("docker", "pull", "alpine/git:user"), "pull git docker image");
    }

    static void pullDockerGitImageOrThrowException() throws InterruptedException {
        log.info("Pulling git image...");
        Response response = pullDockerGitImage();
        if (response.isFailure()) {
            throw new IllegalStateException("Cannot pull docker git image: " + response.toString());
        }
        log.info("Git image pulled");
    }
}