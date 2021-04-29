package io.github.pgagala.gitsynchronizer;

import io.github.pgagala.gitsynchronizer.processexecutor.ProcessExecutor;
import io.github.pgagala.gitsynchronizer.processexecutor.Response;
import lombok.AccessLevel;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.List.of;

/**
 * Creating/destroying temporary git repository (point of file synchronization with remote). Using docker to run git commands.
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Slf4j
//TODO run integration tests on windows os as well
class GitService {

    private static final String DOCKER = "docker";
    private static final List<String> dockerGitInvocationPrefixWithNetwork = of(DOCKER, "run", "--rm", "--network");
    private static final List<String> dockerGitInvocationPrefix = of(DOCKER, "run", "--rm");
    private static final List<String> dockerGitInvocationSuffix = of("-v", getUserHome() + File.separator + ".ssh:/home/git-user/.ssh", "alpine/git:user");
    private static final String ORIGIN = "origin";
    List<String> dockerGitInvocationCommand;
    File gitRepositoryLocalFile;
    GitServerRemote gitServerRemote;
    GitBranch gitBranch;
    ProcessExecutor processExecutor;

    GitService(GitServerRemote serverRemote, GitRepositoryLocal repositoryLocal, GitBranch gitBranch) {
        this.gitRepositoryLocalFile = repositoryLocal.getValue();
        this.gitServerRemote = serverRemote;
        this.gitBranch = gitBranch;
        this.processExecutor = new ProcessExecutor(this.gitRepositoryLocalFile);
        dockerGitInvocationCommand =
            Stream.of(dockerGitInvocationPrefix, of("-v", gitRepositoryLocalFile.getAbsolutePath() + ":/git"), dockerGitInvocationSuffix)
                .flatMap(Collection::stream)
                .collect(Collectors.toUnmodifiableList());
        log.info("!!!sufix: " + dockerGitInvocationSuffix);
    }

    private static String getUserHome() {
        log.info("!!!get env:" + System.getenv("OS"));
        String home =  System.getenv("OS").startsWith("Windows") ?
            System.getenv("USERPROFILE") :
            System.getenv("HOME");
        log.info("!!!home:" + home);
        return home;
    }

    GitService(GitServerRemote serverRemote, GitRepositoryLocal repositoryLocal, GitBranch gitBranch, String gitServerNetwork) {
        this.gitRepositoryLocalFile = repositoryLocal.getValue();
        this.gitServerRemote = serverRemote;
        this.gitBranch = gitBranch;
        this.processExecutor = new ProcessExecutor(repositoryLocal.getValue());
        dockerGitInvocationCommand =
            Stream.of(dockerGitInvocationPrefixWithNetwork, of(gitServerNetwork, "-v", gitRepositoryLocalFile.getAbsolutePath() + ":/git"),
                dockerGitInvocationSuffix)
                .flatMap(Collection::stream)
                .collect(Collectors.toUnmodifiableList());
    }

    void createRepository() throws InterruptedException, IOException {
        createRepositoryFolderIfDoesNotExist();
        log.info("Creating repository under path: {}. Files will be synchronized in that repository. " +
            "After program shutdown that will be automatically cleaned up", gitRepositoryLocalFile.getAbsolutePath());
        Response response = Response.of(initRepository(), addRemote(), createNewBranchAndSwitch());
        if (response.isFailure()) {
            throw new IllegalStateException("Exception during creating repository. Check if docker is running. Response: " + response.result());
        }
    }

    void pull() throws InterruptedException {
        List<String> pullCommand = getDockerGitCommandForLocalExecution(of("pull", ORIGIN, gitBranch.getValue()));
        processExecutor.execute(pullCommand, "git pull", Duration.ofSeconds(5));
    }

    private void createRepositoryFolderIfDoesNotExist() throws IOException {
        if (!gitRepositoryLocalFile.exists()) {
            Files.createDirectory(gitRepositoryLocalFile.toPath());
        }
    }

    private Response initRepository() throws InterruptedException {
        List<String> initCommand = getDockerGitCommandForLocalExecution(of("init"));
        return processExecutor.execute(initCommand, "git init");
    }

    private Response createNewBranchAndSwitch() throws InterruptedException {
        List<String> createNewBranchAndSwitch = getDockerGitCommandForLocalExecution(of("checkout", "-b", gitBranch.getValue()));
        return processExecutor.execute(createNewBranchAndSwitch, "git checkout -b");
    }

    private Response addRemote() throws InterruptedException {
        List<String> addingRemoteCommand = getDockerGitCommandForLocalExecution(of("remote", "add", ORIGIN, gitServerRemote.getValue()));
        return processExecutor.execute(addingRemoteCommand, format("git adding remote %s", gitServerRemote));
    }

    //TODO throw or stay with response ?
    Response deleteRepository() {
        try {
            if (!gitRepositoryLocalFile.exists()) {
                return Response.success();
            }
            FileUtils.forceDelete(gitRepositoryLocalFile);
            return Response.success();
        } catch (IOException exc) {
            String errorMsg = String.format("Unsuccessful deleting repository under path: %s. Error msg: %s",
                gitRepositoryLocalFile.getAbsolutePath(),
                exc.getMessage());
            log.error(errorMsg);
            return Response.failure(errorMsg);
        }
    }

    Response commitChanges(FileChanges fileChanges) throws InterruptedException {
        List<String> addCommand = getDockerGitCommandForLocalExecution(of("add", "."));
        Response addingResp = processExecutor.execute(addCommand, "git adding file");

        if (lackOfNewChangesInRepository()) {
            return Response.success("No new files changes to commit. File changes are same as already existing in local repository");
        }

        String email = "user.email=git@synchronizer-" + UUID.randomUUID() + ".com";
        List<String> commitCommand = getDockerGitCommandForLocalExecution(of("-c", "user.name='git synchronizer'", "-c", email,
            "commit", "-m",
            getCommitMessage(fileChanges)));
        Response committingResp = processExecutor.execute(commitCommand, "git committing");

        List<String> pushingToOriginCommand = getDockerGitCommandForLocalExecution(of("push", ORIGIN, gitBranch.getValue()));
        Response pushingResp = processExecutor.execute(pushingToOriginCommand, "git pushing to origin");

        return Response.of(addingResp, committingResp, pushingResp);
    }

    boolean lackOfNewChangesInRepository() throws InterruptedException {
        List<String> statusCommand = getDockerGitCommandForLocalExecution(of("status"));
        return processExecutor.execute(statusCommand, "git status").result()
            .contains("nothing to commit");
    }

    private String getCommitMessage(FileChanges fileChanges) {
        StringBuilder commitMessageBuilder = new StringBuilder();
        fileChanges.forEach(f -> commitMessageBuilder.append(f.toString()).append(" ").append(System.lineSeparator()));
        return commitMessageBuilder.toString();
    }

    private List<String> getDockerGitCommandForLocalExecution(List<String> gitCommand) {
        List<String> dockerCommand = new ArrayList<>(dockerGitInvocationCommand);
        dockerCommand.addAll(gitCommand);

        return dockerCommand;
    }
}

@Value
class GitServerRemote {
    String value;
}

@Value
class GitRepositoryLocal {
    File value;
}

@Value
class GitBranch {

    public static final GitBranch DEFAULT_BRANCH = new GitBranch("master");

    String value;
}