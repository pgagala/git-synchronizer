package io.github.pgagala;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

//TODO - add gui javafx (https://openjfx.io/)
@Slf4j
public class GitSynchronizerApplication {
    // paths to listening on defined via args to main, commit interval as well. SSH should be earlier set up.
    // recognize if something is file or folder path.toFile().isFile()
    // watching on windows available only on folder lvl - check linux
    // 2 threads - one gathering and flatting changes to queue(getting rid of 2 events after modifying one file - one modified timestamp, seconds
    // modified content)
    // second committing and removing changes from queue with defined intervals to repo (parameter to be configured)

    //repo on remote should be manually set up to application we should only pass repo identifiers,
    //app should copy watched files to local repo and committing straight to remote repo, after app shutdown local repo should be
    //removed ? (depends on option maybe)

    //if nothing is on remote repo all watched files on start should be copied to that repo (synchronization although will be make
    // only after updating watched file)

    @SuppressWarnings("java:S3655")
    public static void main(String[] args) throws IOException, InterruptedException {
        GitSynchronizerApplicationArgsParser appArgs = new GitSynchronizerApplicationArgsParser(args);
        printStartMsg(appArgs);

        GitRepositoryLocal gitRepositoryLocal = appArgs.repositoryLocal();
        GitService gitService = appArgs.network().isPresent() ?
            new GitService(appArgs.serverRemote(), gitRepositoryLocal, appArgs.gitBranch(), appArgs.network().get()) :
            new GitService(appArgs.serverRemote(), gitRepositoryLocal, appArgs.gitBranch());
        RepositoryBootstrap repositoryBootstrap = new RepositoryBootstrap(gitService);
        FileWatcher fileWatcher = new FileWatcher(FileSystems.getDefault().newWatchService(), appArgs.paths());
        FileManager fileManager = new FileManager(gitRepositoryLocal.getValue());
        FileSynchronizer fileSynchronizer = new FileSynchronizer(fileWatcher, gitService, fileManager);
        ExecutorService executorService = Executors.newFixedThreadPool(2,
            new ThreadFactoryBuilder().setNameFormat("git-synchronizer-app-%d").build());

        repositoryBootstrap.initialize();
        executorService.submit(fileWatcher::run);
        executorService.submit(fileSynchronizer::run);

        addShutdownHook(repositoryBootstrap);
        log.info("Git synchronizer started");
    }

    private static void addShutdownHook(RepositoryBootstrap repositoryBootstrap) {
        Runtime.getRuntime().addShutdownHook(new Thread(repositoryBootstrap::cleanup));
    }

    private static void printStartMsg(GitSynchronizerApplicationArgsParser appArgs) throws IOException {
        String startMsg =
            String.format("""
                    Git synchronizer starting with following parameters:
                    - server remote : %s
                    - watching paths: %s
                    - repository path: %s
                    - git branch: %s
                    - git server network: %s
                    """, appArgs.serverRemote().getValue(), appArgs.paths(), appArgs.repositoryLocal().getValue(), appArgs.gitBranch().getValue(),
                appArgs.network().orElse(""));
        log.info(startMsg);
    }

    private static class GitSynchronizerApplicationArgsParser {

        private final ApplicationArgs applicationArgs = new ApplicationArgs();

        GitSynchronizerApplicationArgsParser(@NonNull String[] args) {
            JCommander cmd = JCommander.newBuilder()
                .addObject(applicationArgs)
                .build();
            cmd.parse(args);
        }

        GitServerRemote serverRemote() {
            return new GitServerRemote(applicationArgs.gitServerRemote);
        }

        List<Path> paths() {
            applicationArgs.paths = applicationArgs.paths.stream()
                .distinct()
                .collect(Collectors.toUnmodifiableList());
            return Collections.unmodifiableList(applicationArgs.paths);
        }

        GitRepositoryLocal repositoryLocal() throws IOException {
            return applicationArgs.gitRepositoryPath != null ?
                new GitRepositoryLocal(new File(applicationArgs.gitRepositoryPath)) :
                new GitRepositoryLocal(Files.createTempDirectory("git-synchronizer-temp-repository").toFile());
        }

        GitBranch gitBranch() {
            return applicationArgs.gitBranch != null ? new GitBranch(applicationArgs.gitBranch) : GitBranch.DEFAULT_BRANCH;
        }

        Optional<String> network() {
            return applicationArgs.network != null ? Optional.of(applicationArgs.network) : Optional.empty();
        }

        private static class ApplicationArgs {
            @Parameter(
                names = {"--gitServerRemote", "-g"},
                required = true,
                arity = 1,
                description = "Git server remote where backup of file changes should be stored (e.g. --gitServerRemote git@github" +
                    ".com:pgagala/git-synchronizer.git)",
                validateWith = GitServerRemoteValidator.class
            )
            private String gitServerRemote;


            @Parameter(
                names = {"--paths", "-p"},
                converter = PathConverter.class,
                validateWith = PathValidator.class,
                required = true,
                description = "Paths with files which should be monitored (e.g. --paths /c/myDirToMonitor /c/mySecondDirToMonitor"
            )
            private List<Path> paths;

            @Parameter(
                names = {"--repositoryPath", "-r"},
                arity = 1,
                description = "Repository path under which backup of file changes should be stored (e.g. --repositoryPath /tmp/mySynchronizedRepo)." +
                    " Default is somewhere in tmp folder",
                validateWith = PathValidator.class
            )
            private String gitRepositoryPath;

            @Parameter(
                names = {"--branch", "-b"},
                arity = 1,
                description = "Git branch on which backup of file changes should be committed (e.g. --branch myBackupBranch). Default is master"
            )
            private String gitBranch;

            @Parameter(
                names = {"--network", "-n"},
                arity = 1,
                description = "Optional docker network. Default is none"
            )
            private String network;

        }
    }

    private static class PathConverter implements IStringConverter<Path> {

        @Override
        public Path convert(String path) {
            return Paths.get(path);
        }
    }

    public static class PathValidator implements IParameterValidator {

        private static final String PATH = "^(/[^/ ]*)+/?$";

        @Override
        public void validate(String name, String value) {
            if (!value.matches(PATH)) {
                throw new ParameterException("Passed path doesn't follow pattern: " + PATH);
            }
        }
    }

    public static class GitServerRemoteValidator implements IParameterValidator {

        private static final String GIT_SERVER_REMOTE = "^[^,]+/[^,]+\\.git$";

        @Override
        public void validate(String name, String value) {
            if (!value.matches(GIT_SERVER_REMOTE)) {
                throw new ParameterException("Passed git server remote doesn't follow pattern: " + GIT_SERVER_REMOTE);
            }
        }
    }
}