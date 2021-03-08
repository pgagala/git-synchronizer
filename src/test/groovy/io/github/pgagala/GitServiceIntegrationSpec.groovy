package io.github.pgagala

import io.github.pgagala.util.FileManager
import org.apache.commons.io.FileUtils

import java.nio.file.Files

import static org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils.randomAlphabetic

class GitServiceIntegrationSpec extends IntegrationSpec {

    public static final GitServerRemote GIT_REMOTE = new GitServerRemote("http://$gitServerIp/test_repository.git")

    File testFolder
    FileManager fileManager
    ProcessExecutor processExecutor

    def setup() {
        def testRepoFolderName = "test_repo_" + randomAlphabetic(4)
        testFolder = Files.createTempDirectory(testRepoFolderName).toFile()
        fileManager = new FileManager(testRepoFolderName)
        processExecutor = new ProcessExecutor(testFolder)
    }

    def cleanup() {
        if (testFolder.exists()) {
            FileUtils.forceDelete(testFolder)
        }
    }

    def "Git repository should be created and deleted"() {
        given: "Git service for random path"
            GitService gitService = new GitService(GIT_REMOTE, new GitRepositoryLocal(testFolder), GitService.DEFAULT_BRANCH, gitServerNetwork)

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
        given: "Initialized git service for a random path and branch"
            def newBranch = new GitBranch("branch_${randomAlphabetic(4)}")
            GitService gitService = new GitService(GIT_REMOTE, new GitRepositoryLocal(testFolder), newBranch, gitServerNetwork)
            gitService.createRepository()

        and: "New file copied in repository"
            def fileName = "/file-" + randomAlphabetic(5)
            File file = new File(testFolder.getPath() + fileName)
            assert !file.exists()
            file.createNewFile()

        when: "File is committed to repository"
            def response = gitService.commitChanges(new FileChanges([new FileCreated(file)]))

        then: "File should be successfully committed"
            response.isSuccessful()

        and: "Proper log message should be saved"
            assertGitLogContains(file, "created", newBranch)

        when: "File is modified"
            file.append("file modification")

        and: "File is committed to repository"
            response = gitService.commitChanges(new FileChanges([new FileModified(file)]))

        then: "File should be successfully committed"
            response.isSuccessful()

        and: "Proper log message should be saved"
            assertGitLogContains(file, "changed", newBranch)

        when: "File is deleted"
            file.delete()

        and: "File is committed to repository"
            response = gitService.commitChanges(new FileChanges([new FileDeleted(file)]))

        then: "File should be successfully committed"
            response.isSuccessful()

        and: "Proper log message should be saved"
            assertGitLogContains(file, "deleted", newBranch)
    }

    private boolean assertGitLogContains(File file, String logEvent, GitBranch branch = GitService.DEFAULT_BRANCH) {
        processExecutor.execute(["cat", "${testFolder.getAbsolutePath()}/.git/logs/refs/heads/$branch.value".toString()], "cat file")
                .result()
                .contains("File $logEvent: ${file.getAbsolutePath()}")
    }

}