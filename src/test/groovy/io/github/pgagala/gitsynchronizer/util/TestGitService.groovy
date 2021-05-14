package io.github.pgagala.gitsynchronizer.util

import io.github.pgagala.gitsynchronizer.Environment
import io.github.pgagala.gitsynchronizer.GitBranch
import io.github.pgagala.gitsynchronizer.GitRepositoryLocal
import io.github.pgagala.gitsynchronizer.GitServerRemote
import io.github.pgagala.gitsynchronizer.processexecutor.ProcessExecutor
import io.github.pgagala.gitsynchronizer.processexecutor.Response

@SuppressWarnings("GroovyAccessibility")
class TestGitService {
    private final GitRepositoryLocal executionLocation
    private final String network

    TestGitService(GitRepositoryLocal executionLocation, String network) {
        this.executionLocation = executionLocation
        this.network = network
    }

    Response cloneRepository(GitServerRemote remote, GitRepositoryLocal location = executionLocation, GitBranch branch = GitBranch.DEFAULT_BRANCH) throws InterruptedException {
        def cmd = dockerGitPrefix(location.value) + ["clone", remote.value, "-b", branch.value]
        return new ProcessExecutor(executionLocation.getValue()).execute(cmd, "git clone")
    }

    Response pull(File location = executionLocation.value) throws InterruptedException {
        def cmd = dockerGitPrefix(location) +
                ["pull", "origin"]
        return new ProcessExecutor(location).execute(cmd, "git pull")
    }

    Response log(File location = executionLocation.value) throws InterruptedException {
        def cmd = dockerGitPrefix(location) +
                ["log"]
        return new ProcessExecutor(location).execute(cmd, "git log")
    }

    def dockerGitPrefix(File location) {
        [
                "docker",
                "run",
                "--rm",
                "--network",
                network,
                "-v",
                "${location.getPath()}:/git".toString(),
                "-v",
                "${Environment.getUserHome()}/.ssh:/root/.ssh".toString(),
                "alpine/git:latest"
        ]
    }
}
