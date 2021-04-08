package io.github.pgagala;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.String.format;

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
            try {
                while (true) {
                    FileChanges fileChanges = fileWatcher.occurredFileChanges();
                    if (fileChanges.isEmpty()) {
                        Thread.sleep(1000);
                        continue;
                    }
                    log.info("New file changes occurred on watched paths:\n{}", fileChanges);
                    fileManager.copy(fileChanges.files());
                    gitService.commitChanges(fileChanges);
                }
            } catch (Exception e) {
                Thread.currentThread().interrupt();
                log.error(format("Exception during synchronizing files.%nException: %s", e));
                throw e;
            }
        });
    }
}