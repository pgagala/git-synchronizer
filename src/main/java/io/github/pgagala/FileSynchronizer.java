package io.github.pgagala;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
class FileSynchronizer {

    ExecutorService executorService =
        Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("file-synchronizer-thread-%d").build());
    FileWatcher fileWatcher;
    GitService gitService;

    void run() throws IOException, InterruptedException {
        executorService.submit(() -> {
            while (true) {
                FileChanges fileChanges = fileWatcher.occurredFileChanges();
                if(fileChanges.isEmpty()) {
                    continue;
                }
                for (val fileChange : fileChanges) {
                    System.out.println("File change: " + fileChange.toString());
                }
                gitService.commitChanges(fileChanges);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}