package io.github.pgagala

import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path


class FileManagerSpec extends Specification {

    @Shared
    FileManager fileManager
    String fileManagerTargetPath

    def setup() {
        def targetFile = Files.createTempDirectory("test_repo_" + RandomStringUtils.randomAlphabetic(4) + "_").toFile()
        targetFile.deleteOnExit()
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
            targetPathFile.listFiles().toList() == files

        where:
            files << [[dirWithContent()], [file()], [dirWithContent(), file()]]
            size = files.size()
    }

    static File dirWithContent() {
        def dir = Files.createTempDirectory("testDir_" + RandomStringUtils.randomAlphabetic(4)).toFile()
        dir.deleteOnExit()
        def dir2 = Files.createDirectory(Path.of(dir.getAbsolutePath().toString(), "/subDir")).toFile()
        dir2.deleteOnExit()
        def file = new File(dir2, "testFile")
        file.createNewFile()
        file.deleteOnExit()

        return dir
    }

    static File file() {
        def file = Files.createTempFile("tesFile_${RandomStringUtils.randomAlphabetic(4)}", RandomStringUtils.randomAlphabetic(4)).toFile()
        file.createNewFile()
        file.deleteOnExit()

        return file
    }

}