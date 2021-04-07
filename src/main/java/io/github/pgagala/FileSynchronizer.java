package io.github.pgagala;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Slf4j
class FileSynchronizer {

    ExecutorService executorService =
        Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("file-synchronizer-thread-%d").build());
    FileWatcher fileWatcher;
    GitService gitService;
    FileManager fileManager;

    void run() {
        executorService.submit(() -> {
            while (true) {
                FileChanges fileChanges = fileWatcher.occurredFileChanges();
                if (fileChanges.isEmpty()) {
                    pause();
                    continue;
                }
                log.info("New file changes occurred on watching paths: {}", fileChanges);
                fileManager.copy(fileChanges.files());
                gitService.commitChanges(fileChanges);
            }
        });
    }

    private void pause() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.error("Error occurred during synchronizing files", e);
            Thread.currentThread().interrupt();
        }
    }
}