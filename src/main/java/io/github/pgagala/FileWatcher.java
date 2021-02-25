package io.github.pgagala;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
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
class FileWatcher {
    //TODO integration to test changes from different places
    //TODO cannot add files with same name from different paths
    Map<String, Function<WatchEvent<?>, FileChange>> eventNameToFileChangeCreatorMapping = Map.of(
        ENTRY_CREATE.name(), FileCreated::of,
        ENTRY_MODIFY.name(), FileModified::of,
        ENTRY_DELETE.name(), FileDeleted::of);
    LinkedBlockingQueue<WatchEvent<?>> linkedBlockingQueue = new LinkedBlockingQueue<>();
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
        }
    }

    void run() {
        executorService.submit(() -> {
            boolean poll = true;
            while (poll) {
                WatchKey key;
                try {
                    key = watchService.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    continue;
                }
                List<WatchEvent<?>> watchEvents = key.pollEvents();
                if (watchEvents == null) {
                    continue;
                }
                linkedBlockingQueue.addAll(watchEvents);
                poll = key.reset();
            }
        });
    }

    //TODO
    //
    //then test on windows
    //then acceptance integration spec
    //testing ?
    //optionally javafx
    FileChanges occurredFileChanges() {
        List<WatchEvent<?>> events = new ArrayList<>();
        linkedBlockingQueue.drainTo(events);
        return toFileChanges(removeDuplicatedEvents(events));
    }

    private FileChanges toFileChanges(List<WatchEvent<?>> events) {
        val fileChangesList = events
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

        return new FileChanges(fileChangesList);
    }

    private List<WatchEvent<?>> removeDuplicatedEvents(List<WatchEvent<?>> watchEvents) {
        return watchEvents.stream()
            .reduce(new ArrayList<>(),
                (flattenEvents, event) -> {
                    if (flattenEvents.stream()
                        .noneMatch(ev -> ev.context().equals(event.context()) && ev.kind().equals(event.kind()))) {
                        flattenEvents.add(event);
                    }
                    return flattenEvents;
                },
                (flattenEvents, events) -> {
                    flattenEvents.addAll(events);
                    return flattenEvents;
                });
    }
}