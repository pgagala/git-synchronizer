package io.github.pgagala

import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path

import static org.apache.commons.io.FileUtils.forceDelete

class FileManagerSpec extends Specification {

    FileManager fileManager
    String fileManagerTargetPath

    def setup() {
        def targetFile = Files.createTempDirectory("testRepo_" + RandomStringUtils.randomAlphabetic(4) + "_").toFile()
        fileManagerTargetPath = targetFile.getAbsolutePath()
        fileManager = new FileManager(fileManagerTargetPath)
    }

    def "File should be removed"() {
        given: "Existing file"
            assert file.exists()

        when: "File is removed"
            fileManager.delete(file)

        then: "File doesn't exist"
            !file.exists()

        cleanup:
            if(file.exists()) {forceDelete(file)}

        where:
            file << [dirWithContent(), file()]
    }

    def "Files should be copied to target path"() {
        given: "Lack of files under target path"
            def targetPathFile = new File(fileManagerTargetPath)
            assert targetPathFile.listFiles().size() == 0

        when: "Files are copied to target path"
            fileManager.copy(files)

        then: "Files exist under target path"
            targetPathFile.listFiles().size() == size
            targetPathFile.listFiles().each {
                for (File file : files)
                    it.shallowEquals(file)
            }

        cleanup:
            files.each { forceDelete(it) }
            forceDelete(targetPathFile)

        where:
            files << [[dirWithContent(), file()], [dirWithContent()], [file()]]
            size = files.size()
    }

    File dirWithContent() {
        def dir = Files.createTempDirectory("testDir_" + RandomStringUtils.randomAlphabetic(4)).toFile()
        Files.createDirectory(Path.of(dir.getAbsolutePath().toString(), "/subDir")).toFile()

        return dir
    }

    File file() {
        def file = Files.createTempFile("testFile_${RandomStringUtils.randomAlphabetic(4)}", RandomStringUtils.randomAlphabetic(4)).toFile()
        file.createNewFile()

        return file
    }

}