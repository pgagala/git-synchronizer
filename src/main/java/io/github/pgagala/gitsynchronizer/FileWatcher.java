package io.github.pgagala.gitsynchronizer;

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
import java.util.function.Supplier;
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
    Map<WatchKey, Path> watchKeyWatchedFolderMap;
    Map<WatchKey, List<File>> watchKeyWatchedFileMap;
    IgnoredFiles ignoredFiles;

    public FileWatcher(WatchService watchService, List<Path> paths, Function<File, Collection<File>> filesFetcher, IgnoredFiles ignoredFiles) throws IOException {
        this.watchService = watchService;
        executorService = Executors.newFixedThreadPool(paths.size(), new ThreadFactoryBuilder().setNameFormat("file-watcher-thread-%d").build());
        this.filesFetcher = filesFetcher;
        this.watchKeyWatchedFolderMap = new HashMap<>();
        this.watchKeyWatchedFileMap = new HashMap<>();
        this.ignoredFiles = ignoredFiles;
        subscribePathsToWatcherService(Collections.unmodifiableList(paths));
    }

    public FileWatcher(WatchService watchService, List<Path> paths) throws IOException {
        this(watchService, paths, f -> FileUtils.listFiles(f, null, false), IgnoredFiles.intermediateIgnoredFiles());
    }

    private void subscribePathsToWatcherService(List<Path> paths) throws IOException {
        for (Path path : paths) {
            if (path.toFile().isFile()) {
                subscribeSingleFile(path);
                addFileToInitialFileInitializedEvents(path.toFile());
            } else {
                watchKeyWatchedFolderMap.put(path.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE), path);
                addFilesToInitialFileInitializedEvents(path);
            }
        }
    }

    private void subscribeSingleFile(Path path) throws IOException {
        Path parentPathOfFile = path.toFile().getParentFile().toPath();
        WatchKey watchKey = parentPathOfFile.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
        watchKeyWatchedFileMap.merge(watchKey, List.of(path.toFile()), (l1, l2) -> {
            List<File> files = new ArrayList<>();
            files.addAll(l1);
            files.addAll(l2);
            return Collections.unmodifiableList(files);
        });
    }

    private void addFilesToInitialFileInitializedEvents(Path path) {
        filesFetcher.apply(path.toFile())
            .stream()
            .filter(File::isFile)
            .filter(f -> !ignoredFiles.shouldBeIgnored(f))
            .forEach(this::addFileToInitialFileInitializedEvents);
    }

    private void addFileToInitialFileInitializedEvents(File file) {
        FileInitialized fileInitialized = FileInitialized.of(file);
        if (fileChanges.contains(fileInitialized)) {
            log.error("There is already a synchronized file with same name as: " + fileInitialized.fileName());
            throw new DuplicatedWatchedFileException("There is already a synchronized file with same name as: " + fileInitialized);
        }
        fileChanges.add(fileInitialized);
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
                    List<WatchEvent<?>> reducedWatchEvents = ignoredFiles.removeEventsRefersToIgnoredFiles(watchEvents);
                    if (watchKeyWatchedFileMap.containsKey(key)) {
                        addToFileChangesForWatchedSingleFile(key, reducedWatchEvents);
                    }
                    if (watchKeyWatchedFolderMap.containsKey(key)) {
                        fileChanges.addAll(toFileChanges(reducedWatchEvents, watchKeyWatchedFolderMap.get(key)));
                    }
                    poll = key.reset();
                }
            } catch (Exception e) {
                Thread.currentThread().interrupt();
                log.error(format("Exception during watching paths: %s, watching files: %s for file events. %n Exception: %s",
                    watchKeyWatchedFolderMap.values(),
                    watchKeyWatchedFileMap.values(), e));
                throw new IllegalStateException(e);
            }
        });
    }

    private void addToFileChangesForWatchedSingleFile(WatchKey key, List<WatchEvent<?>> watchEvents) {
        Supplier<String> correspondingSingleFileErrorMsg = () -> format(
            """
                Cannot find corresponding single file watched for: 
                watchKeyWatchedFileMap: %s,
                watchEvents: %s
                """, watchKeyWatchedFileMap, toHumanReadable(watchEvents));
        File correspondingFile = watchKeyWatchedFileMap.get(key)
            .stream()
            .filter(f -> watchEvents.stream().anyMatch(e -> e.context().toString().equals(f.getName())))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(correspondingSingleFileErrorMsg.get()));

        Supplier<String> correspondingSingleEventErrorMsg = () -> format(
            """
                Cannot find corresponding single event for:
                corresponding file: %s,
                watchEvents: %s
                """, correspondingFile, toHumanReadable(watchEvents));
        WatchEvent<?> correspondingEvent = watchEvents.stream()
            .filter(e -> e.context().toString().equals(correspondingFile.getName()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(correspondingSingleEventErrorMsg.get()));

        fileChanges.add(eventNameToFileChangeCreatorMapping.get(correspondingEvent.kind().name()).apply(correspondingFile));
    }

    private List<String> toHumanReadable(List<WatchEvent<?>> watchEvents) {
        return watchEvents.stream()
            .map(e -> "type: " + e.kind().name() + ", name: " + e.context().toString())
            .collect(Collectors.toUnmodifiableList());
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
    private static class Event {
        String kind;
        String path;
    }

    private Event event(WatchEvent<?> event, Path path) {
        return new Event(event.kind().name(), path.toString() + "/" + event.context().toString());
    }
}