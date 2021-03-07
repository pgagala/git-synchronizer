package io.github.pgagala.util

import io.github.pgagala.ProcessExecutor
import io.github.pgagala.Response

class TestGitService {
    private final File executionLocation
    private final String network

    def dockerGitPrefix(File location) {
        [
                "docker",
                "run",
                "--rm",
                "--network",
                network,
                "-v",
                "$location:/git".toString(),
                "-v",
                "${System.getenv("HOME")}.ssh:/root/.ssh".toString(),
                "alpine/git:user"
        ]
    }

    TestGitService(File executionLocation, String network) {
        this.executionLocation = executionLocation
        this.network = network
    }

    Response cloneRepository(String repository, File location = executionLocation, String branch = "master") throws InterruptedException {
        def cmd = dockerGitPrefix(location) + ["clone", repository, "-b", branch]
        return new ProcessExecutor(executionLocation).execute(cmd, "git clone")
    }

    Response pull(File location = executionLocation) throws InterruptedException {
        def cmd = dockerGitPrefix(location) +
                ["pull", "origin"]
        return new ProcessExecutor(location).execute(cmd, "git pull")
    }

    Response createNewBranchAndSwitch(String branchName, File location = executionLocation) {
        def cmd = dockerGitPrefix(location) + ["checkout", "-b", branchName]
        return new ProcessExecutor(location).execute(cmd, "git checkout -b")
    }

}
