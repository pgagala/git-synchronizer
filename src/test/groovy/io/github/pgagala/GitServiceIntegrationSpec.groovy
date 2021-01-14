package io.github.pgagala

import org.apache.commons.io.FileUtils
import spock.lang.Shared

import java.nio.file.Files

import static org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils.randomAlphabetic

class GitServiceIntegrationSpec extends IntegrationSpec {

    public static final String GIT_REMOTE = "http://127.0.0.1:$gitServicePort/test_repository.git"

    @Shared
    File testFolder

    @Shared
    FileManager fileManager

    @Shared
    ProcessExecutor processExecutor

    def setup() {
        def testRepoPath = "test_repo_" + randomAlphabetic(4)
        testFolder = Files.createTempDirectory(testRepoPath).toFile()
        fileManager = new FileManager(testRepoPath)
        processExecutor = new ProcessExecutor(testFolder)
    }

    def cleanup() {
        if (testFolder.exists()) {
            FileUtils.forceDelete(testFolder)
        }
    }

    def "Git repository should be created and deleted"() {
        given: "Git service for random path"
            GitService gitService = new GitService(testFolder.getPath(), GIT_REMOTE)

        when: "Git is initialized"
            gitService.createRepository()

        then: "Git repository exists"
            testFolder.listFiles().any { it.getName() == '.git' }

        when: "Git repository is removed"
            gitService.deleteRepository()

        then: "Git repository doesn't exist"
            testFolder.listFiles() == null
    }

    def "Committed files should be present on connected remote git server"() {
        given: "Initialized git service for a random path"
            GitService gitService = new GitService(testFolder.getPath(), GIT_REMOTE)
            gitService.createRepository()

        and: "New file copied in repository"
            def fileName = "/file-" + randomAlphabetic(5)
            File file = new File(testFolder.getPath() + fileName)
            assert !file.exists()
            file.createNewFile()

        when: "File is committed to repository"
            def response = gitService.commitChanges(new FilesChanges([new CreationFileChange(file)]))

        then: "File should be successfully committed"
            response.isSuccessful()

        and: "Proper log message should be saved"
            processExecutor.execute(["cat", "${testFolder.getAbsolutePath()}/.git/logs/refs/heads/master".toString()], "cat file")
                    .result()
                    .contains("File created: " + file.getAbsolutePath())

        when: "File is modified"

        and: "File is committed to repository"

        then: "File should be successfully committed"

        and: "Proper log message should be saved"

        when: "File is deleted"

        and: "File is committed to repository"

        then: "File should be successfully committed"

        and: "Proper log message should be saved"

    }

}