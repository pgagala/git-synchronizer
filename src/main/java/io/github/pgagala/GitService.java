package io.github.pgagala;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Creating/destroying temporary git repository (point of file synchronization with remote). Using docker to run git commands.
 */
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Slf4j
class GitService {

    private static final String DOCKER = "docker";
    private static final String[] DOCKER_GIT_INVOCATION_PREFIX = {DOCKER, "run", "--name", "git-service", "alpine/git"};
    String gitRepositoryPath;
    String gitServerRemote;

    void createRepository() throws IOException, InterruptedException {
        log.info("Creating repository under path: {}. Files will be synchronized in that repository. " +
            "After program shutdown that will be automatically cleaned up", gitRepositoryPath);
        try {
            initRepository();
            copyRepositoryToLocalHost();
            addRemote();
        } catch (Exception exc) {
            log.error("Exception during creating repository: {}", exc.getMessage());
            throw exc;
        }
    }

    private void addRemote() throws IOException, InterruptedException {
        executeProcess(new ProcessBuilder()
            .command(DOCKER, "remote", "add", "origin", gitServerRemote));
    }

    private void removeGitContainer() throws IOException, InterruptedException {
        executeProcess(new ProcessBuilder()
            .command(DOCKER, "rm", "git-service"));
    }

    private void copyRepositoryToLocalHost() throws IOException, InterruptedException {
        executeProcess(new ProcessBuilder()
            .command(DOCKER, "cp", "git-service:git/.git", gitRepositoryPath));
    }

    private void initRepository() throws IOException, InterruptedException {
        executeProcess(new ProcessBuilder()
            .command(getDockerGitCommand("init")));
    }

    void deleteRepository() throws IOException, InterruptedException {
        File repositoryFolder = new File(gitRepositoryPath);
        try {
            FileUtils.forceDelete(repositoryFolder);
            removeGitContainer();
        } catch (IOException | InterruptedException exc) {
            log.error("Unsuccessful deleting repository under path: {}", gitRepositoryPath, exc);
            throw exc;
        }
    }

    void commitChanges(FilesChanges fileChanges) throws IOException, InterruptedException {
        StringBuilder commitMessageBuilder = new StringBuilder();
        fileChanges.forEach(f -> commitMessageBuilder.append(f.getLogMessage()).append("/n"));

        executeProcess(new ProcessBuilder()
            .command(getDockerGitCommand("add .")));

        executeProcess(new ProcessBuilder()
            .command(getDockerGitCommand("commit -m " + commitMessageBuilder.toString())));

        executeProcess(new ProcessBuilder()
            .command(getDockerGitCommand("push origin")));
    }

    private static String[] getDockerGitCommand(String gitCommand) {
        String[] commandSuffix = gitCommand.split(" ");
        String[] fullCommand = Arrays.copyOf(DOCKER_GIT_INVOCATION_PREFIX, DOCKER_GIT_INVOCATION_PREFIX.length + commandSuffix.length);
        System.arraycopy(commandSuffix, 0, fullCommand, DOCKER_GIT_INVOCATION_PREFIX.length, commandSuffix.length);
        return fullCommand;
    }

    private void executeProcess(ProcessBuilder processBuilder) throws IOException, InterruptedException {
        executeAndWaitUntilFinished(processBuilder.start());
    }

    private void executeAndWaitUntilFinished(Process process) throws InterruptedException {

        int responseCode = process.waitFor();
        if (responseCode != 0) {
            log.error("Unsuccessful process execution: {}", process);
        }
    }
}