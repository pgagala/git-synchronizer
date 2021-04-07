package io.github.pgagala;

class DuplicatedWatchedFileException extends IllegalArgumentException {
    DuplicatedWatchedFileException(String s) {
        super(s);
    }
}