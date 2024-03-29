package io.github.pgagala.gitsynchronizer;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Copying/removing files to/from specific path.
 *
 * @author Paweł Gągała
 */
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
class FileManager {

    File targetFilePath;

    FileManager(String targetPath) {
        this.targetFilePath = new File(targetPath);
    }

    void deleteFromTargetPath(List<String> fileNames) {
        fileNames.forEach(this::delete);
    }

    void delete(File file) {
        try {
            FileUtils.forceDelete(file);
        } catch (IOException exc) {
            log.error("Unsuccessful deleting file: {}.", file.getAbsolutePath(), exc);
        }
    }

    void delete(String fileName) {
        File fileToDelete = new File(targetFilePath.getAbsolutePath() + File.separator + fileName);
        try {
            FileUtils.forceDelete(fileToDelete);
        } catch (IOException exc) {
            log.error("Unsuccessful deleting file: {}.", fileToDelete.getAbsolutePath(), exc);
            throw new IllegalStateException(String.format("Unsuccessful deleting file: %s.", fileToDelete.getAbsolutePath()));
        }
    }

    void copy(List<File> files) {
        files.forEach(this::copy);
    }

    void copy(File file) {
        try {
            if (file.isDirectory()) {
                FileUtils.copyDirectoryToDirectory(file, targetFilePath);
            } else {
                FileUtils.copyFileToDirectory(file, targetFilePath, true);
            }
        } catch (IOException exc) {
            log.error("Unsuccessful copying file: {} to path: {}.", file.getAbsolutePath(), targetFilePath.getAbsolutePath(), exc);
        }
    }

}