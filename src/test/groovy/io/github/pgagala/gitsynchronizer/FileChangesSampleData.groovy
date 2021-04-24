package io.github.pgagala.gitsynchronizer

import io.github.pgagala.gitsynchronizer.FileChange
import io.github.pgagala.gitsynchronizer.FileChanges
import io.github.pgagala.gitsynchronizer.FileCreated
import io.github.pgagala.gitsynchronizer.FileDeleted
import io.github.pgagala.gitsynchronizer.FileModified
import sun.nio.fs.AbstractWatchKey

import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent

@SuppressWarnings("GroovyAccessibility")
trait FileChangesSampleData {

    FileChanges fileChanges(List<FileChange> fileChanges) {
        return new FileChanges(fileChanges)
    }

    FileInitialized fileInitialized(File file) {
        return FileInitialized.of(file)
    }

    FileModified fileModified(File file) {
         return FileModified.of(file)
    }

    FileCreated fileCreated(File file) {
        return FileCreated.of(file)
    }

    FileDeleted fileDeleted(File file) {
        return FileDeleted.of(file)
    }

    WatchEvent<?> eventModify(File file) {
        return eventModify(file.name)
    }

    WatchEvent<?> eventModify(String fileName) {
        return new AbstractWatchKey.Event<Object>(StandardWatchEventKinds.ENTRY_MODIFY, fileName)
    }

    WatchEvent<?> eventCreate(File file) {
        return eventCreate(file.name)
    }

    WatchEvent<?> eventCreate(String fileName) {
        return new AbstractWatchKey.Event<Object>(StandardWatchEventKinds.ENTRY_CREATE, fileName)
    }

    WatchEvent<?> eventDelete(File file) {
        return eventDelete(file.name)
    }

    WatchEvent<?> eventDelete(String fileName) {
        return new AbstractWatchKey.Event<Object>(StandardWatchEventKinds.ENTRY_DELETE, fileName)
    }
}
