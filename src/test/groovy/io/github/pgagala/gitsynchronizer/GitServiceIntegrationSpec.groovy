package io.github.pgagala.gitsynchronizer

import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import spock.lang.Timeout

import java.nio.file.Files
import java.util.concurrent.TimeUnit

import static org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils.randomAlphabetic

@Timeout(value = 3, unit = TimeUnit.MINUTES)
@SuppressWarnings("GroovyAccessibility")
@Slf4j
class GitServiceIntegrationSpec extends IntegrationSpec {

    public static final String SEPARATOR = File.separator
    public static final GitServerRemote GIT_REMOTE = new GitServerRemote("http://$gitServerIp/test_repository.git")

    File testFolder
    FileManager fileManager

    def setup() {
        def testRepoFolderName = "test_repo_" + randomAlphabetic(4)
        testFolder = Files.createTempDirectory(testRepoFolderName).toFile()
        fileManager = new FileManager(testRepoFolderName)
    }

    def cleanup() {
        try {
            if (testFolder.exists()) {
                FileUtils.forceDelete(testFolder)
            }
        }
        catch (Throwable ignored) {
        }
    }

    def "Git repository should be created and deleted"() {
        given: "Git service for random path"
            GitService gitService = new GitService(GIT_REMOTE, new GitRepositoryLocal(testFolder), GitBranch.DEFAULT_BRANCH, gitServerNetwork)

        when: "Git is initialized"
            gitService.createRepository()

        then: "Git repository exists"
        log.info("files before: " + testFolder.listFiles())
            testFolder.listFiles().any { it.getName() == '.git' }

        when: "Git repository is removed"
            gitService.deleteRepository()

        then: "Git repository doesn't exist"
        log.info("files after: " + testFolder.listFiles())
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
            def response = gitService.commitChanges(new FileChanges([FileCreated.of(file)]))

        then: "File should be successfully committed"
            response.isSuccessful()

        and: "Proper log message should be saved"
            assertGitLogContains(file, "created", newBranch)

        when: "File is modified"
            file.append("file modification")

        and: "File is committed to repository"
            response = gitService.commitChanges(new FileChanges([FileModified.of(file)]))

        then: "File should be successfully committed"
            response.isSuccessful()

        and: "Proper log message should be saved"
            assertGitLogContains(file, "changed", newBranch)

        when: "File is deleted"
            file.delete()

        and: "File is committed to repository"
            response = gitService.commitChanges(new FileChanges([FileDeleted.of(file)]))

        then: "File should be successfully committed"
            response.isSuccessful()

        and: "Proper log message should be saved"
            assertGitLogContains(file, "deleted", newBranch)
    }

    def "Commit should be successful if there are no changes in repository at all"() {
        given: "Initialized git service for a random path and branch"
            def newBranch = new GitBranch("branch_${randomAlphabetic(4)}")
            GitService gitService = new GitService(GIT_REMOTE, new GitRepositoryLocal(testFolder), newBranch, gitServerNetwork)
            gitService.createRepository()

        when: "File is committed to repository"
            def response = gitService.commitChanges(new FileChanges([]))
        then: "commit is successful"
            response.isSuccessful()

        when: "New file is copied to repository"
            def fileName = "/file-" + randomAlphabetic(5)
            File file = new File(testFolder.getPath() + fileName)
            assert !file.exists()
            file.createNewFile()
            response = gitService.commitChanges(new FileChanges([FileCreated.of(file)]))
        then: "commit is successful"
            response.isSuccessful()

        when: "Same file is committed"
            response = gitService.commitChanges(new FileChanges([FileCreated.of(file)]))
        then: "commit is successful"
            response.isSuccessful()
    }

    private boolean assertGitLogContains(File file, String logEvent, GitBranch branch = GitBranch.DEFAULT_BRANCH) {
        def path = "${SEPARATOR}.git${SEPARATOR}logs${SEPARATOR}refs${SEPARATOR}heads${SEPARATOR}$branch.value"
        Files.readString(new File("${testFolder.getAbsolutePath()}$path").toPath())
            .contains("File $logEvent: ${file.getAbsolutePath()}")
    }

}