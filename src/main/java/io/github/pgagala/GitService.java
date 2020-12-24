package io.github.pgagala;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.List.of;

/**
 * Creating/destroying temporary git repository (point of file synchronization with remote). Using docker to run git commands.
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Slf4j
//TODO run integration tests on windows as well
class GitService {

    private static final String DOCKER = "docker";
    private static final List<String> dockerGitInvocationPrefix = of(DOCKER, "run", "--rm", "-v");
    private static final List<String> dockerGitInvocationSuffix = of("-v", System.getenv("HOME") + "/.ssh:/root/.ssh", "alpine/git:user");
    List<String> dockerGitInvocationCommand;
    File gitRepositoryFile;
    String gitServerRemote;

    GitService(String gitRepositoryPath, String gitServerRemote) {
        this.gitRepositoryFile = new File(gitRepositoryPath);
        this.gitServerRemote = gitServerRemote;
        dockerGitInvocationCommand =
            Stream.of(dockerGitInvocationPrefix, of(gitRepositoryPath + ":/git"), dockerGitInvocationSuffix)
                .flatMap(Collection::stream)
                .collect(Collectors.toUnmodifiableList());
    }

    void createRepository() throws IOException, InterruptedException {
        log.info("Creating repository under path: {}. Files will be synchronized in that repository. " +
            "After program shutdown that will be automatically cleaned up", gitRepositoryFile.getAbsolutePath());
        try {
            initRepository();
            addRemote();
        } catch (Exception exc) {
            log.error("Exception during creating repository: {}", exc.getMessage());
            throw exc;
        }
    }

    private void addRemote() throws IOException, InterruptedException {
        executeProcess(String.format("git adding remote %s", gitServerRemote), new ProcessBuilder().directory(gitRepositoryFile)
            .command(DOCKER, "remote", "add", "origin", gitServerRemote));
    }

    private void initRepository() throws IOException, InterruptedException {
        executeProcess("git init", new ProcessBuilder()
            .command(getDockerGitCommandForLocalExecution(of("init"))));
    }

    void deleteRepository() throws IOException {
        try {
            FileUtils.forceDelete(gitRepositoryFile);
        } catch (IOException exc) {
            log.error("Unsuccessful deleting repository under path: {}", gitRepositoryFile.getAbsolutePath(), exc);
            throw exc;
        }
    }

    void commitChanges(FilesChanges fileChanges) throws IOException, InterruptedException {
        StringBuilder commitMessageBuilder = new StringBuilder();
        fileChanges.forEach(f -> commitMessageBuilder.append(f.getLogMessage()).append("/n"));
        String commitMessage = commitMessageBuilder.substring(0, commitMessageBuilder.lastIndexOf("/n"));

        executeProcess("git adding file", new ProcessBuilder().directory(gitRepositoryFile)
            .command(getDockerGitCommandForLocalExecution(of("add", "."))));

        executeProcess("git committing", new ProcessBuilder().directory(gitRepositoryFile)
            .command(getDockerGitCommandForLocalExecution(of("-c", "user.name='haker bonzo'", "-c", "user.email=hakier@bonzo.pl", "commit", "-m",
                commitMessage))));

        executeProcess("git pushing to origin", new ProcessBuilder().directory(gitRepositoryFile)
            .command(getDockerGitCommandForLocalExecution(of("push", "origin"))));
    }

    private String[] getDockerGitCommandForLocalExecution(List<String> gitCommand) {
        List<String> dockerCommand = new ArrayList<>(dockerGitInvocationCommand);
        dockerCommand.addAll(gitCommand);

        return dockerCommand.toArray(new String[0]);
    }

    private void executeProcess(String description, ProcessBuilder processBuilder) throws IOException, InterruptedException {
        executeAndWaitUntilFinished(description, processBuilder.start());
    }

    private void executeAndWaitUntilFinished(String description, Process process) throws InterruptedException {
        int responseCode = process.waitFor();
        if (responseCode != 0) {
            log.error("Unsuccessful {} process execution: {}", description, process);
        }
    }
}