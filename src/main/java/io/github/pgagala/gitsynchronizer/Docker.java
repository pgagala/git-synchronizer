package io.github.pgagala.gitsynchronizer;

import io.github.pgagala.gitsynchronizer.processexecutor.ProcessExecutor;
import io.github.pgagala.gitsynchronizer.processexecutor.Response;

import java.io.File;
import java.util.List;

class Docker {

    static void startGitUserImageOrThrowException() throws InterruptedException {
        Response response = startGitUser();
        if (response.isFailure()) {
            throw new IllegalStateException("Cannot start docker git image: " + response.toString());
        }
    }

    static Response startGitUser() throws InterruptedException {
        return new ProcessExecutor(new File("./")).execute(List.of("docker", "load", "-i", "./docker_images/git_image_latest.tar.gz"), "loading git image");
    }
}