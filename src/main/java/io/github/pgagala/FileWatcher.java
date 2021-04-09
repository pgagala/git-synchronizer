package io.github.pgagala;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Slf4j
class FileWatcher {
    Map<String, Function<File, FileChange>> eventNameToFileChangeCreatorMapping = Map.of(
        ENTRY_CREATE.name(), FileCreated::of,
        ENTRY_MODIFY.name(), FileModified::of,
        ENTRY_DELETE.name(), FileDeleted::of);
    LinkedBlockingQueue<FileChange> fileChanges = new LinkedBlockingQueue<>();
    ExecutorService executorService;
    WatchService watchService;
    Function<File, Collection<File>> filesFetcher;
    Map<WatchKey, Path> watchKeysPathMap;

    public FileWatcher(WatchService watchService, List<Path> paths, Function<File, Collection<File>> filesFetcher) throws IOException {
        this.watchService = watchService;
        executorService = Executors.newFixedThreadPool(paths.size(), new ThreadFactoryBuilder().setNameFormat("file-watcher-thread-%d").build());
        this.filesFetcher = filesFetcher;
        this.watchKeysPathMap = new HashMap<>();
        subscribePathsToWatcherService(Collections.unmodifiableList(paths));
    }

    public FileWatcher(WatchService watchService, List<Path> paths) throws IOException {
        this(watchService, paths, f -> FileUtils.listFiles(f, null, false));
    }

    private void subscribePathsToWatcherService(List<Path> paths) throws IOException {
        for (Path path : paths) {
            watchKeysPathMap.put(path.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE), path);
            addFilesToInitialFileCreatedEvents(path);
        }
    }

    private void addFilesToInitialFileCreatedEvents(Path path) {
        filesFetcher.apply(path.toFile())
            .stream()
            .filter(File::isFile)
            .forEach(f -> {
                    FileCreated fileCreated = FileCreated.of(f);
                    if (fileChanges.contains(fileCreated)) {
                        log.error("There is already a synchronized file with same name as: " + fileCreated.fileName());
                        throw new DuplicatedWatchedFileException("There is already a synchronized file with same name as: " + fileCreated);
                    }
                    fileChanges.add(fileCreated);
                }
            );
    }

    void run() {
        executorService.submit(() -> {
            try {
                boolean poll = true;
                while (poll) {
                    WatchKey key = watchService.take();
                    List<WatchEvent<?>> watchEvents = key.pollEvents();
                    if (watchEvents == null) {
                        continue;
                    }
                    fileChanges.addAll(toFileChanges(watchEvents, watchKeysPathMap.get(key)));
                    poll = key.reset();
                }
                //TODO if throwing that here will be shown in 102 ?
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error(format("Interrupted exception during watching paths: %s for file events. %n Exception: %s", watchKeysPathMap.values(), e));
                throw new IllegalStateException(e.getMessage(), e);
            } catch (Exception e) {
                Thread.currentThread().interrupt();
                log.error(format("Exception during watching paths: %s for file events. %n Exception: %s", watchKeysPathMap.values(), e));
                throw e;
            }
        });
    }

    FileChanges occurredFileChanges() {
        List<FileChange> changes = new ArrayList<>();
        fileChanges.drainTo(changes);

        return new FileChanges(removeDuplicatedFileChanges(changes));
    }

    private List<FileChange> toFileChanges(List<WatchEvent<?>> events, Path path) {
        return events
            .stream()
            .peek(event -> {
                    String eventName = event.kind().name();
                    if (!eventNameToFileChangeCreatorMapping.containsKey(eventName)) {
                        throw new IllegalArgumentException("Unsupported event name: " + eventName);
                    }
                }
            )
            .map(event -> event(event, path))
            .filter(event -> {
                File f = new File(event.path);
                return f.isFile() || !f.exists();
            })
            .map(
                event ->
                    eventNameToFileChangeCreatorMapping.get(event.kind()).apply(new File(event.path)))
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

    @Value
    @Accessors(fluent = true)
    private class Event {
        String kind;
        String path;
    }

    private Event event(WatchEvent<?> event, Path path) {
        return new Event(event.kind().name(), path.toString() + "/" + event.context().toString());
    }
}