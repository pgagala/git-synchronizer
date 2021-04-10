package io.github.pgagala.gitsynchronizer;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.io.File;
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

    List<File> newOrModifiedFiles() {
        return changes.stream()
            .filter(f -> !f.connectedWithRemoval())
            .map(FileChange::file)
            .collect(Collectors.toUnmodifiableList());
    }

    List<File> deletedFiles() {
        return changes.stream()
            .filter(FileChange::connectedWithRemoval)
            .map(FileChange::file)
            .collect(Collectors.toUnmodifiableList());
    }
}

interface FileChange {
    File file();
    default boolean connectedWithRemoval() {
        return false;
    }
}

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Accessors(fluent = true)
class FileModified implements FileChange {

    static FileModified of(File file) {
        return new FileModified(file, file.getName());
    }

    @Getter
    File file;

    @Getter
    @EqualsAndHashCode.Include
    String fileName;

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

    static FileCreated of(File file) {
        return new FileCreated(file, file.getName());
    }

    @Getter
    File file;

    @Getter
    @EqualsAndHashCode.Include
    String fileName;

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

    static FileDeleted of(File file) {
        return new FileDeleted(file, file.getName());
    }

    @Override
    public boolean connectedWithRemoval() {
        return true;
    }

    @Getter
    File file;

    @Getter
    @EqualsAndHashCode.Include
    String fileName;

    @Override
    public String toString() {
        return format("File deleted: %s", file.getAbsolutePath());
    }
}