package io.github.pgagala;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.io.IOException;
import java.nio.file.WatchEvent;
import java.util.List;
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
                List<WatchEvent<?>> watchEvents = fileWatcher.occurredEvents();
                for (WatchEvent<?> event : watchEvents) {
                    System.out.println("Event kind : " + event.kind() + " - File : " + event.context());
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