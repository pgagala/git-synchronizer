package io.github.pgagala.gitsynchronizer;

import io.github.pgagala.gitsynchronizer.processexecutor.ProcessExecutor;
import io.github.pgagala.gitsynchronizer.processexecutor.Response;

import java.io.File;
import java.util.List;

class Docker {

    static Response downloadDockerGitImage() throws InterruptedException {
        return new ProcessExecutor(new File(Environment.getUserHome())).execute(List.of("docker", "pull", "alpine/git:user"), "download git docker " +
            "image");
    }

    static void downloadDockerGitImageOrThrowException() throws InterruptedException {
        Response response = downloadDockerGitImage();
        if (response.isFailure()) {
            throw new IllegalStateException("Cannot download docker git image: " + response.toString());
        }
    }
}