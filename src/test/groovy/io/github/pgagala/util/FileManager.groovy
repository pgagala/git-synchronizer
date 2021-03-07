package io.github.pgagala.util

import org.apache.commons.io.FileUtils

import static org.apache.commons.io.FileUtils.forceDeleteOnExit
//TODO adjust logging
class FileManager {

    File targetFilePath

    FileManager(String targetPath) throws IOException {
        this.targetFilePath = new File(targetPath)
        forceDeleteOnExit(targetFilePath)
    }

    void delete(List<File> files) {
        files.forEach(this::delete)
    }

    void delete(File file) {
        try {
            FileUtils.forceDelete(file)
        } catch (IOException exc) {
            log.error("Unsuccessful deleting file: {}.", file.getAbsolutePath(), exc);
        }
    }

    void copy(List<File> files) {
        files.forEach(this::copy)
    }

    void copy(File file) {
        try {
            if (file.isDirectory()) {
                FileUtils.copyDirectoryToDirectory(file, targetFilePath)
            } else {
                FileUtils.copyFileToDirectory(file, targetFilePath, true)
            }
        } catch (IOException exc) {
            log.error("Unsuccessful copying file: {} to path: {}.", file.getAbsolutePath(), targetFilePath.getAbsolutePath(), exc)
        }
    }

}