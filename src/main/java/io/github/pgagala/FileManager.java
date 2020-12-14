package io.github.pgagala;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Slf4j
class FileManager {

    File targetFilePath;

    FileManager(String targetPath) {
        this.targetFilePath = new File(targetPath);
    }

    void delete(File file) {
        try {
            FileUtils.forceDelete(file);
        } catch (IOException exc) {
            log.error("Unsuccessful deleting file: {}.", file.getAbsolutePath(), exc);
        }
    }

    void copy(List<File> files) {
        files.forEach(file -> {
            try {
                if(file.isDirectory()) {
                    FileUtils.copyDirectory(file, targetFilePath);
                }
                else {
                    FileUtils.copyFile(file, targetFilePath);
                }
            } catch (IOException exc) {
                log.error("Unsuccessful copying file: {} to path: {}.", file.getAbsolutePath(), targetFilePath.getAbsolutePath(), exc);
            }
        });
    }

}