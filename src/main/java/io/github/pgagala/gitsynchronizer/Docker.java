package io.github.pgagala.gitsynchronizer;

import io.github.pgagala.gitsynchronizer.processexecutor.ProcessExecutor;
import io.github.pgagala.gitsynchronizer.processexecutor.Response;

import java.io.File;
import java.util.List;

class Docker {

    static Response buildDockerGitImage() throws InterruptedException {
        return new ProcessExecutor(new File("").getAbsoluteFile())
            .execute(List.of("docker", "build", "./", "-f", "./docker/git.Dockerfile", "-t", "git"), "build git docker image");
    }

    static void buildDockerGitImageOrThrowException() throws InterruptedException {
        Response response = buildDockerGitImage();
        if (response.isFailure()) {
            throw new IllegalStateException("Cannot build docker git image: " + response.toString());
        }
    }
}