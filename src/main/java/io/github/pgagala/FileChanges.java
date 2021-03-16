package io.github.pgagala;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.WatchEvent;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static java.lang.String.format;

@ToString
@EqualsAndHashCode
class FileChanges implements Iterable<FileChange> {

    private final List<FileChange> changes;

    FileChanges(List<FileChange> changes) {
        this.changes = Collections.unmodifiableList(changes);
    }

    //TODO check if unmodifable
    @Override
    public @NotNull Iterator<FileChange> iterator() {
        return changes.iterator();
    }

    boolean isEmpty() {
        return this.changes.isEmpty();
    }
}

interface FileChange {
    String getLogMessage();
    Object getFileName();
}


@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@ToString
@EqualsAndHashCode
class FileModified implements FileChange {

    static FileModified of(WatchEvent<?> event) {
        return new FileModified(event.context());
    }

    @Getter
    Object fileName;

    @Override
    public String getLogMessage() {
        return format("File changed: %s", fileName);
    }
}

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@ToString
@EqualsAndHashCode
class FileCreated implements FileChange {

    static FileCreated of(WatchEvent<?> event) {
        return new FileCreated(event.context());
    }

    static FileCreated of(File file) {
        return new FileCreated(file.getName());
    }

    @Getter
    Object fileName;

    @Override
    public String getLogMessage() {
        return format("File created: %s", fileName);
    }
}

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@ToString
@EqualsAndHashCode
class FileDeleted implements FileChange {

    static FileDeleted of(WatchEvent<?> event) {
        return new FileDeleted(event.context());
    }

    @Getter
    Object fileName;

    @Override
    public String getLogMessage() {
        return format("File deleted: %s", fileName);
    }
}