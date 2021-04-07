package io.github.pgagala.util

import io.github.pgagala.GitBranch
import io.github.pgagala.GitRepositoryLocal
import io.github.pgagala.GitServerRemote
import io.github.pgagala.ProcessExecutor
import io.github.pgagala.Response

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
                "${System.getenv("HOME")}.ssh:/root/.ssh".toString(),
                "alpine/git:user"
        ]
    }
}
