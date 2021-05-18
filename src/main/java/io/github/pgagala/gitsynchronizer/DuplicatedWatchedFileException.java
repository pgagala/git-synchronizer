package io.github.pgagala.gitsynchronizer;

/**
 * @author Paweł Gągała
 */
class DuplicatedWatchedFileException extends IllegalArgumentException {
    DuplicatedWatchedFileException(String s) {
        super(s);
    }
}