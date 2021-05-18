package io.github.pgagala.gitsynchronizer;

import io.github.pgagala.gitsynchronizer.processexecutor.ProcessExecutor;
import io.github.pgagala.gitsynchronizer.processexecutor.Response;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;

/**
 * Building docker images used in application
 *
 * @author Paweł Gągała
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class Docker {

    static Response buildDockerGitImage() throws InterruptedException {
        return new ProcessExecutor(new File("").getAbsoluteFile())
            .execute(List.of("docker", "build", "./", "-f", "./docker/git.Dockerfile", "-t", "git"), "build git docker image");
    }

    static void buildDockerGitImageOrThrowException() throws InterruptedException {
        log.info("Building git image...");
        Response response = buildDockerGitImage();
        if (response.isFailure()) {
            throw new IllegalStateException("Cannot build docker git image: " + response.toString());
        }
        log.info("Git image built");
    }
}