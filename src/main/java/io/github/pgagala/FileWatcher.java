package io.github.pgagala;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

class FileWatcher {

    private final LinkedBlockingQueue<WatchEvent<?>> linkedBlockingQueue = new LinkedBlockingQueue<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("file-watcher-thread-%d").build());
    private final WatchService watchService;

    public FileWatcher(WatchService watchService) {
        this.watchService = watchService;
    }

    void run() throws IOException {
        Path path = Paths.get("c:\\Trash");
        Path path2 = Paths.get("c:\\Trash\\file_.txt");
        path.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
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
                        .noneMatch(ev -> ev.context().equals(event.context()))) {
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