package io.github.pgagala;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.nio.file.WatchEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static java.lang.String.format;

@ToString
@EqualsAndHashCode
class FileChanges implements Iterable<FileChange> {

    private final List<FileChange> fileChanges;

    public FileChanges(List<FileChange> fileChanges) {
        this.fileChanges = Collections.unmodifiableList(fileChanges);
    }

    @Override
    public Iterator<FileChange> iterator() {
        return new ArrayList<>(fileChanges).iterator();
    }

    FileChanges add(FileChange fileChange) {
        List<FileChange> copiedElements = new ArrayList<>(fileChanges);
        copiedElements.add(fileChange);

        return new FileChanges(copiedElements);
    }

    FileChanges add(FileChanges fileChangesToAdd) {
        ArrayList<FileChange> newFileChanges = new ArrayList<>(this.fileChanges);
        newFileChanges.addAll(fileChangesToAdd.fileChanges);

        return new FileChanges(newFileChanges);
    }
}

interface FileChange {
    String getLogMessage();
}


@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode
@ToString
class FileModified implements FileChange {

    static FileModified of(WatchEvent<?> event) {
        return new FileModified(event.context());
    }

    @Getter
    Object context;

    @Override
    public String getLogMessage() {
        return format("File changed: %s", context);
    }
}

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode
@ToString
class FileCreated implements FileChange {

    static FileCreated of(WatchEvent<?> event) {
        return new FileCreated(event.context());
    }

    @Getter
    Object context;

    @Override
    public String getLogMessage() {
        return format("File created: %s", context);
    }
}

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode
@ToString
class FileDeleted implements FileChange {

    static FileDeleted of(WatchEvent<?> event) {
        return new FileDeleted(event.context());
    }

    @Getter
    Object context;

    @Override
    public String getLogMessage() {
        return format("File deleted: %s", context);
    }
}