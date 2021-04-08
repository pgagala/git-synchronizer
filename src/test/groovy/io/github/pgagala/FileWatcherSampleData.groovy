package io.github.pgagala

import sun.nio.fs.AbstractWatchKey

import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds

trait FileWatcherSampleData {

    FileChanges fileChanges(List<FileChange> fileChanges) {
        return new FileChanges(fileChanges)
    }

    FileModified fileModified(File file) {
         return FileModified.of(eventModify(file), Path.of("/"))
    }

    FileCreated fileCreated(File file) {
        return FileCreated.of(eventCreate(file), Path.of("/"))
    }

    FileDeleted fileDeleted(File file) {
        return FileDeleted.of(eventDelete(file), Path.of("/"))
    }

    def eventModify(File file) {
        return new AbstractWatchKey.Event<Object>(StandardWatchEventKinds.ENTRY_MODIFY, file.name)
    }

    def eventCreate(File file) {
        return new AbstractWatchKey.Event<Object>(StandardWatchEventKinds.ENTRY_CREATE, file.name)
    }

    def eventDelete(File file) {
        return new AbstractWatchKey.Event<Object>(StandardWatchEventKinds.ENTRY_DELETE, file.name)
    }

}
