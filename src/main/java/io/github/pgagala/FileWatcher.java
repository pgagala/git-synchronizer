package io.github.pgagala;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Slf4j
class FileWatcher {
    //TODO integration to test changes from different places
    //TODO cannot add files with same name from different paths
    Map<String, Function<WatchEvent<?>, FileChange>> eventNameToFileChangeCreatorMapping = Map.of(
        ENTRY_CREATE.name(), FileCreated::of,
        ENTRY_MODIFY.name(), FileModified::of,
        ENTRY_DELETE.name(), FileDeleted::of);
    LinkedBlockingQueue<FileChange> fileChanges = new LinkedBlockingQueue<>();
    ExecutorService executorService;
    WatchService watchService;

    public FileWatcher(WatchService watchService, List<Path> paths) throws IOException {
        this.watchService = watchService;
        executorService = Executors.newFixedThreadPool(paths.size(), new ThreadFactoryBuilder().setNameFormat("file-watcher-thread-%d").build());
        subscribePathsToWatcherService(Collections.unmodifiableList(paths));
    }

    private void subscribePathsToWatcherService(List<Path> paths) throws IOException {
        for (Path path : paths) {
            path.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
            addFilesToInitialFileCreatedEvents(path);
        }
    }

    private void addFilesToInitialFileCreatedEvents(Path path) {
         Arrays.stream(path.toFile()
             .listFiles())
             .iterator()
             .forEachRemaining(f -> fileChanges.add(FileCreated.of(f)));
    }

    void run() {
        executorService.submit(() -> {
            boolean poll = true;
            while (poll) {
                WatchKey key;
                try {
                    key = watchService.take();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                List<WatchEvent<?>> watchEvents = key.pollEvents();
                if (watchEvents == null) {
                    continue;
                }
                fileChanges.addAll(toFileChanges(watchEvents));
                poll = key.reset();
            }
        });
    }

    FileChanges occurredFileChanges() {
        List<FileChange> changes = new ArrayList<>();
        fileChanges.drainTo(changes);

        return new FileChanges(removeDuplicatedFileChanges(changes));
    }

    private List<FileChange> toFileChanges(List<WatchEvent<?>> events) {
        return events
            .stream()
            .peek(event -> {
                    String eventName = event.kind().name();
                    if (!eventNameToFileChangeCreatorMapping.containsKey(eventName)) {
                        throw new IllegalArgumentException("Unsupported event name: " + eventName);
                    }
                }
            )
            .map(event -> eventNameToFileChangeCreatorMapping.get(event.kind().name()).apply(event))
            .collect(Collectors.toUnmodifiableList());
    }

    private List<FileChange> removeDuplicatedFileChanges(List<FileChange> fileChanges) {
        return fileChanges.stream()
            .reduce(new ArrayList<>(),
                (uniqueFileChanges, fileChange) -> {
                    if (uniqueFileChanges.stream()
                        .noneMatch(existingFileChange -> existingFileChange.equals(fileChange))) {
                        uniqueFileChanges.add(fileChange);
                    }
                    return uniqueFileChanges;
                },
                (uniqueFileChanges, fileChangesToAdd) -> {
                    uniqueFileChanges.addAll(fileChangesToAdd);
                    return uniqueFileChanges;
                });
    }
}