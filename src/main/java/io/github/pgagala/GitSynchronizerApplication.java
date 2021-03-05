package io.github.pgagala;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import lombok.NonNull;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

//TODO - add gui javafx (https://openjfx.io/)
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

    public static void main(String[] args) throws IOException, InterruptedException {
        GitSynchronizerApplicationArgsParser appArgs = new GitSynchronizerApplicationArgsParser(args);

        FileWatcher fileWatcher = new FileWatcher(FileSystems.getDefault().newWatchService(), appArgs.paths());
        fileWatcher.run();

        GitService gitService = new GitService(appArgs.serverRemote());
        new FileSynchronizer(fileWatcher, gitService).run();

        System.out.println("App started");

    }

    private static class GitSynchronizerApplicationArgsParser {

        private final ApplicationArgs applicationArgs = new ApplicationArgs();

        GitSynchronizerApplicationArgsParser(@NonNull String[] args) {
            JCommander cmd = JCommander.newBuilder( )
                .addObject(applicationArgs)
                .build();
            cmd.parse(args);
        }

        String serverRemote() {
            return applicationArgs.gitServerRemote;
        }

        List<Path> paths() {
            applicationArgs.paths = applicationArgs.paths.stream()
                .distinct()
                .collect(Collectors.toUnmodifiableList());
            return Collections.unmodifiableList(applicationArgs.paths);
        }

        private class ApplicationArgs {
            @Parameter(
                names = {"--gitServerRemote", "-g"},
                required = true,
                arity = 1,
                description = "Git server remote when backup of file changes should be stored (e.g. --gitServerRemote git@github" +
                    ".com:pgagala/git-synchronizer.git)",
                validateWith = GitServerRemoteValidator.class
            )
            private String gitServerRemote;

            @Parameter(
                names = {"--paths", "-p"},
                converter = PathConverter.class,
                validateWith = PathValidator.class,
                required= true,
                description = "Paths with files which should be monitored (e.g. --paths /c/myDirToMonitor /c/mySecondDirToMonitor"
            )
            private List<Path> paths;
        }
    }

    private static class PathConverter implements IStringConverter<Path> {

        @Override
        public Path convert(String path) {
            return Paths.get(path);
        }
    }

    public static class PathValidator implements IParameterValidator {

        private static final String PATH = "^(\\/[^\\/ ]*)+\\/?$";

        @Override
        public void validate(String name, String value) {
            if (!value.matches(PATH)) {
                throw new ParameterException("Passed path doesn't follow pattern: " + PATH);
            }
        }
    }

    public static class GitServerRemoteValidator implements IParameterValidator {

        private static final String GIT_SERVER_REMOTE = "^git@[^,:]+\\.[^,]+:[^,.]+\\.git$";

        @Override
        public void validate(String name, String value) {
            if (!value.matches(GIT_SERVER_REMOTE)) {
                throw new ParameterException("Passed git server remote doesn't follow pattern: " + GIT_SERVER_REMOTE);
            }
        }
    }
}