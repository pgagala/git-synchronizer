package io.github.pgagala;

import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static java.lang.String.format;

@RequiredArgsConstructor
class FilesChanges implements Iterable<FileChange> {

    private final List<FileChange> fileChanges;

    @Override
    public Iterator<FileChange> iterator() {
        return new ArrayList<>(fileChanges).iterator();
    }

    FilesChanges add(FileChange fileChange) {
        List<FileChange> copiedElements = new ArrayList<>(fileChanges);
        copiedElements.add(fileChange);

        return new FilesChanges(copiedElements);
    }
}

interface FileChange {
    String getLogMessage();
}


@Value
class ModificationFileChange implements FileChange {
    File file;

    @Override
    public String getLogMessage() {
        return format("File changed: %s", file.getAbsolutePath());
    }
}

@Value
class CreationFileChange implements FileChange {
    File file;

    @Override
    public String getLogMessage() {
        return format("File created: %s", file.getAbsolutePath());
    }
}

@Value
class DeletionFileChange implements FileChange {
    File file;

    @Override
    public String getLogMessage() {
        return format("File deleted: %s", file.getAbsolutePath());
    }
}