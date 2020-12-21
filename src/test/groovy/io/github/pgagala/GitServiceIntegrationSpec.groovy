package io.github.pgagala

import org.apache.commons.io.FileUtils
import spock.lang.Shared

import java.nio.file.Files

import static org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils.randomAlphabetic

class GitServiceIntegrationSpec extends IntegrationSpec {

    public static final String GIT_REMOTE = "http://127.0.0.1:$gitServicePort/test_repository.git"

    @Shared
    File folderUnderRandomPath

    @Shared
    FileManager fileManager

    def setup() {
        def testRepoPath = "test_repo_" + randomAlphabetic(4)
        folderUnderRandomPath = Files.createTempDirectory(testRepoPath).toFile()
        fileManager = new FileManager(testRepoPath)
    }

    def cleanup() {
        if (folderUnderRandomPath.exists()) {
            FileUtils.forceDelete(folderUnderRandomPath)
        }
    }

    def "Git repository should be created and deleted"() {
        given: "Git service for random path"
            GitService gitService = new GitService(folderUnderRandomPath.getPath(), GIT_REMOTE)

        when: "Git is initialized"
            gitService.createRepository()

        then: "Git repository exists"
            folderUnderRandomPath.listFiles().any { it.getName() == '.git' }

        when: "Git repository is removed"
            gitService.deleteRepository()

        then: "Git repository doesn't exist"
            folderUnderRandomPath.listFiles() == null
    }

    def "Committed files should be present on connected remote git server"() {
        given: "Initialized git service for a random path"
            GitService gitService = new GitService(folderUnderRandomPath.getPath(), GIT_REMOTE)
            gitService.createRepository()

        and: "New file copied in repository"
            def fileName = "/file-" + randomAlphabetic(5)
            File file = new File(folderUnderRandomPath.getPath() + fileName)
            assert !file.exists()
            file.createNewFile()

        when: "File is committed to repository"
            gitService.commitChanges(new FilesChanges([new CreationFileChange(file)]))

        then: "File should be successfully committed"
            printf ""

        and: "Proper log message should be saved"

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