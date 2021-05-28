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

    private static final int MAX_PULL_AMOUNT = 10;

    static Response pullDockerGitImage() throws InterruptedException {
        return new ProcessExecutor(new File("./"))
            .execute(List.of("docker", "pull", "alpine/git:user"), "pull git docker image");
    }

    static void pullDockerGitImageOrThrowException() throws InterruptedException {
        int pullAttempt = 1;
        Response response = Response.failure("");
        while (pullAttempt <= MAX_PULL_AMOUNT) {
            log.info("Pulling git image for {}/{} time...", pullAttempt, MAX_PULL_AMOUNT);
            response = pullDockerGitImage();
            if (response.isSuccessful()) {
                log.info("Git image pulled");
                return;
            } else {
                log.info("Error during pulling docker git image for {}/{} time: {}.", pullAttempt, MAX_PULL_AMOUNT, response.toString());
            }
            pullAttempt++;
            Thread.sleep(2000);
        }
        String errorMsg = String.format("Cannot pull docker git image: %s. Please try manually pull that image (docker pull alpine/git:user)", response.toString());
        throw new IllegalStateException(errorMsg);
    }
}