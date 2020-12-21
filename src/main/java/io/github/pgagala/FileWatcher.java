package io.github.pgagala;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

class FileWatcher {
    //TODO integration to test changes from differents places
    //TODO cannot add files with same name from different paths
    private final LinkedBlockingQueue<WatchEvent<?>> linkedBlockingQueue = new LinkedBlockingQueue<>();
    private final ExecutorService executorService;
    private final WatchService watchService;

    public FileWatcher(WatchService watchService, List<Path> paths) throws IOException {
        this.watchService = watchService;
        executorService = Executors.newFixedThreadPool(paths.size(), new ThreadFactoryBuilder().setNameFormat("file-watcher-thread-%d").build());
        registerWatcherService(Collections.unmodifiableList(paths));
    }

    private void registerWatcherService(List<Path> paths) throws IOException {
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

    List<WatchEvent<?>> occurredEvents() {
        List<WatchEvent<?>> events = new ArrayList<>();
        linkedBlockingQueue.drainTo(events);
        return removeDuplicatedEvents(events);
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