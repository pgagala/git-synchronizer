package io.github.pgagala;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.val;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class FileSynchronizer {

    ExecutorService executorService =
        Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("file-synchronizer-thread-%d").build());
    private final FileWatcher fileWatcher;

    public FileSynchronizer(FileWatcher fileWatcher) {
        this.fileWatcher = fileWatcher;
    }

    void run() throws IOException, InterruptedException {
        executorService.submit(() -> {
            while (true) {
                FileChanges fileChanges = fileWatcher.occurredFileChanges();
                for (val fileChange : fileChanges) {
                    System.out.println("File change: " + fileChange.toString());
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}