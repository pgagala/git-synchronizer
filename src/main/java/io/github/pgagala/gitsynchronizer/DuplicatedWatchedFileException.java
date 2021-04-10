package io.github.pgagala.gitsynchronizer;

class DuplicatedWatchedFileException extends IllegalArgumentException {
    DuplicatedWatchedFileException(String s) {
        super(s);
    }
}