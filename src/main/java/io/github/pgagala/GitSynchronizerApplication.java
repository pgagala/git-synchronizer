package io.github.pgagala;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.util.List;

class GitSynchronizerApplication {
    // paths to listening on defined via args to main, commit interval as well. SSH should be earlier set up.ą
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
        FileWatcher fileWatcher = new FileWatcher(FileSystems.getDefault().newWatchService(), List.of(Paths.get("c:\\Trash")));
        fileWatcher.run();
        new FileSynchronizer(fileWatcher).run();
        System.out.println("App started");

    }
}