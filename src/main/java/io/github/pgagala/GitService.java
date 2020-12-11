package io.github.pgagala;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Creating/destroying temporary git repository (point of file synchronization with remote). Using docker to run git commands.
 */
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Slf4j
class GitService {

    String gitRepositoryPath;

    void createRepository() throws IOException, InterruptedException {
        log.info("Creating repository under path: {}. Files will be synchronized in that repository. " +
            "After program shutdown that will be automatically cleaned up", gitRepositoryPath);
        try {
            initRepository();
            copyRepositoryToLocalHost();
            removeGitContainer();
        } catch (Exception exc) {
            log.error("Exception during creating repository: {}", exc.getMessage());
            throw exc;
        }
    }

    private void removeGitContainer() throws IOException, InterruptedException {
        Process removeGitServiceContainer = new ProcessBuilder()
            .command("docker", "rm", "git-service")
            .start();

        executeAndWaitUntilFinished(removeGitServiceContainer);
    }

    private void copyRepositoryToLocalHost() throws IOException, InterruptedException {
        Process copyRepositoryToLocalHost = new ProcessBuilder()
            .command("docker", "cp", "git-service:git/.git", gitRepositoryPath)
            .start();
        executeAndWaitUntilFinished(copyRepositoryToLocalHost);
    }

    private void initRepository() throws IOException, InterruptedException {
        Process initRepository = new ProcessBuilder()
            .command("docker", "run", "--name", "git-service", "alpine/git", "init")
            .start();

        executeAndWaitUntilFinished(initRepository);
    }

    void deleteRepository() throws IOException {
        File repositoryFolder = new File(gitRepositoryPath);
        try {
            FileUtils.forceDelete(repositoryFolder);
        } catch (IOException exc) {
            log.error("Unsuccessful deleting repository under path: {}", gitRepositoryPath);
            throw exc;
        }
    }

    private void executeAndWaitUntilFinished(Process process) throws InterruptedException {
        int responseCode = process.waitFor();
        if (responseCode != 0) {
            log.error("Unsuccessful process execution: {}", process);
        }
    }
}