package io.github.pgagala;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

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

    List<File> files() {
        return changes.stream()
            .filter(FileChange::isFile)
            .map(FileChange::file)
            .collect(Collectors.toUnmodifiableList());
    }
}

interface FileChange {
    boolean isFile();
    File file();
}

class FileChangeUtils {
    static String eventPath(WatchEvent<?> event, Path path) {
        return path.toString() + "/" + event.context().toString();
    }
}

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Accessors(fluent = true)
class FileModified implements FileChange {

    //TODO exception here should be raised
    static FileModified of(WatchEvent<?> event, Path path) {
        String eventPath = FileChangeUtils.eventPath(event, path);
        return new FileModified(new File(eventPath), eventPath);
    }

    static FileModified of(File file) {
        return new FileModified(file, file.toPath().toString());
    }

    @Getter
    File file;

    @Getter
    @EqualsAndHashCode.Include
    String fileName;

    @Override
    public boolean isFile() {
        return file.isFile();
    }

    @Override
    public String toString() {
        return format("File changed: %s", file.getAbsolutePath());
    }
}

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Accessors(fluent = true)
class FileCreated implements FileChange {

    static FileCreated of(WatchEvent<?> event, Path path) {
        String eventPath = FileChangeUtils.eventPath(event, path);
        return new FileCreated(new File(eventPath), eventPath);
    }

    static FileCreated of(File file) {
        return new FileCreated(file, file.getName());
    }

    @Getter
    File file;

    @Getter
    @EqualsAndHashCode.Include
    String fileName;

    @Override
    public boolean isFile() {
        return file.isFile();
    }

    @Override
    public String toString() {
        return format("File created: %s", file.getAbsolutePath());
    }
}

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Accessors(fluent = true)
class FileDeleted implements FileChange {

    static FileDeleted of(WatchEvent<?> event, Path path) {
        String eventPath = FileChangeUtils.eventPath(event, path);
        return new FileDeleted(new File(eventPath), eventPath);
    }

    static FileDeleted of(File file) {
        return new FileDeleted(file, file.getName());
    }

    @Getter
    File file;

    @Getter
    @EqualsAndHashCode.Include
    String fileName;

    @Override
    public boolean isFile() {
        return file.isFile();
    }

    @Override
    public String toString() {
        return format("File deleted: %s", file.getAbsolutePath());
    }
}