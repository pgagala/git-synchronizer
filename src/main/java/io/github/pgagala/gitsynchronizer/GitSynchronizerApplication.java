package io.github.pgagala.gitsynchronizer;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.StringKey;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class GitSynchronizerApplication {

    @SuppressWarnings("java:S3655")
    public static void main(String[] args) throws IOException, InterruptedException, URISyntaxException {
        GitSynchronizerApplicationArgsParser appArgs = new GitSynchronizerApplicationArgsParser(args);
        if(appArgs.applicationArgs.help) {
            return;
        }
        Docker.buildDockerGitImageOrThrowException();
        printStartMsg(appArgs);

        GitRepositoryLocal gitRepositoryLocal = appArgs.repositoryLocal();
        GitService gitService = appArgs.network().isPresent() ?
            new GitService(appArgs.serverRemote(), gitRepositoryLocal, appArgs.gitBranch(), appArgs.network().get()) :
            new GitService(appArgs.serverRemote(), gitRepositoryLocal, appArgs.gitBranch());
        FileWatcher fileWatcher = new FileWatcher(FileSystems.getDefault().newWatchService(), appArgs.paths(), appArgs.ignoredFilesPattern());
        FileManager fileManager = new FileManager(gitRepositoryLocal.getValue());
        RepositoryBootstrap repositoryBootstrap = new RepositoryBootstrap(gitService);
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

    private static void printStartMsg(GitSynchronizerApplicationArgsParser appArgs) {
        String startMsg =
            String.format("""
                    Git synchronizer starting with following parameters:
                    - server remote : %s
                    - watching paths: %s
                    - repository path: %s
                    - git branch: %s
                    - ignored file patterns: %s
                    %s
                    """,
                appArgs.serverRemote().getValue(),
                appArgs.paths(),
                appArgs.repositoryLocal().getValue(),
                appArgs.gitBranch().getValue(),
                appArgs.ignoredFilesPattern(),
                appArgs.network().map(n -> "- git server network: " + n).orElse(""));
        log.info(startMsg);
    }

    private static class GitSynchronizerApplicationArgsParser {

        private static final String HELP = "--help";
        private final ApplicationArgs applicationArgs = new ApplicationArgs();

        GitSynchronizerApplicationArgsParser(@NonNull String[] args) throws URISyntaxException {
            String executionSource =
                new File(GitSynchronizerApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath();
            JCommander cmd = JCommander.newBuilder()
                .addObject(applicationArgs)
                .programName("java -jar " + executionSource)
                .build();
            parse(args, cmd);
        }

        private void parse(@NonNull String[] args, JCommander cmd) {
            try {
                cmd.parse(args);
            } catch (ParameterException exception) {
                log.error("Application invoked with invalid arguments: {}.", (Object) args);
                cmd.usage();
                throw exception;
            }
            if(cmd.getDescriptions().get(new StringKey(HELP)).isAssigned()) {
                cmd.usage();
            }
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

        private final GitRepositoryLocal randomGitRepositoryLocal = new GitRepositoryLocal(
            new File(System.getProperty("java.io.tmpdir") + "/git" + "-synchronizer-temp-repository-" + UUID.randomUUID())
        );

        GitRepositoryLocal repositoryLocal() {
            return applicationArgs.gitRepositoryPath != null ?
                new GitRepositoryLocal(new File(applicationArgs.gitRepositoryPath)) :
                randomGitRepositoryLocal;
        }

        GitBranch gitBranch() {
            return applicationArgs.gitBranch != null ? new GitBranch(applicationArgs.gitBranch) : GitBranch.DEFAULT_BRANCH;
        }

        IgnoredFiles ignoredFilesPattern() {
            return applicationArgs.ignoredPattern != null ? IgnoredFiles.from(applicationArgs.ignoredPattern) :
                IgnoredFiles.intermediateIgnoredFiles();
        }

        Optional<String> network() {
            return applicationArgs.network != null ? Optional.of(applicationArgs.network) : Optional.empty();
        }

        private static class ApplicationArgs {

            @Parameter(names = {HELP, "--h"}, help = true)
            private boolean help;

            @Parameter(
                names = {"--gitServerRemote", "-g"},
                required = true,
                arity = 1,
                description = "Git server remote where backup of file changes should be stored (e.g. --gitServerRemote git@github.com:pgagala/git-synchronizer.git)",
                validateWith = GitServerRemoteValidator.class
            )
            private String gitServerRemote;

            @Parameter(
                names = {"--paths", "-p"},
                required = true,
                converter = PathConverter.class,
                validateWith = PathValidator.class,
                description = """
                    Paths with files which should be monitored (e.g. for unix: "--paths /home/myDirToMonitor,/home/mySecondDirToMonitor" and for windows: \
                    "--paths C:\\myDirToMonitor,C:\\mySecondDirToMonitor" """
            )
            private List<Path> paths;

            @Parameter(
                names = {"--repositoryPath", "-r"},
                arity = 1,
                description = """
                    Repository path under which backup of file changes should be stored (e.g. --repositoryPath /tmp/mySynchronizedRepo).\
                    Default is somewhere in operating system's tmp folder""",
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
                names = {"--ignoredPattern", "-i"},
                description = """
                    Ignored file pattern  (e.g. --ignoredPattern ^bla.*$,^foo.*bar$). Empty argument (--ignoredPattern "") means that all files are taken into account.\
                     Default is %s""" + IgnoredFiles.INTERMEDIATE_FILES_PATTERN,
                converter = IgnoredPatternConverter.class,
                validateWith = IgnoredPatternValidator.class
            )
            private List<Pattern> ignoredPattern;

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

    private static class IgnoredPatternConverter implements IStringConverter<Pattern> {

        @Override
        public Pattern convert(String ignoredPattern) {
            return Pattern.compile(ignoredPattern);
        }
    }

    public static class PathValidator implements IParameterValidator {

        @Override
        public void validate(String name, String value) {
            try {
                boolean allAbsolutes = Arrays.stream(value.split(","))
                    .map(Path::of)
                    .allMatch(Path::isAbsolute);
                if (!allAbsolutes) {
                    throw new ParameterException("Passed path isn't absolute path: " + value);
                }
            } catch (RuntimeException exc) {
                throw new ParameterException("Passed path isn't parsable: " + value);
            }
        }
    }

    public static class IgnoredPatternValidator implements IParameterValidator {

        @Override
        public void validate(String name, String value) {
            try {
                Pattern.compile(value);
            } catch (Exception exc) {
                throw new ParameterException("Passed ignored pattern isn't correct regexp");
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