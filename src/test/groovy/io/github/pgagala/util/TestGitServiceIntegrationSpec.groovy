package io.github.pgagala.util

import io.github.pgagala.FileChanges
import io.github.pgagala.FileCreated
import io.github.pgagala.FileModified
import io.github.pgagala.GitService
import io.github.pgagala.IntegrationSpec
import io.github.pgagala.ProcessExecutor
import org.apache.commons.io.FileUtils
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils

import java.nio.file.Files

import static org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils.randomAlphabetic

class TestGitServiceIntegrationSpec extends IntegrationSpec {

    public static final String GIT_REMOTE = "http://$gitServerIp/test_repository.git"
    public static final String RANDOM_TEXT = "lorem ipsum-${randomAlphabetic(12)}"

    File testFolder
    FileManager fileManager
    def processExecutor
    File clonedTestFolder
    def testGitService

    def setup() {
        def testRepoFolderName = "test_repo_" + randomAlphabetic(4)
        testFolder = Files.createTempDirectory(testRepoFolderName).toFile()
        fileManager = new FileManager(testRepoFolderName)
        processExecutor = new ProcessExecutor(testFolder)

        def clonedTestRepoFolderName = "cloned_test_repo_" + randomAlphabetic(4)
        clonedTestFolder = Files.createTempDirectory(clonedTestRepoFolderName).toFile()
        testGitService = new TestGitService(testFolder, gitServerNetwork)
    }

    def cleanup() {
        if (testFolder.exists()) {
            FileUtils.forceDelete(testFolder)
        }
    }

    def "Repository should be cloned"() {
        given: "repository with random branch"
            def gitService = createRepository()
            def newBranch = "branch_${RandomStringUtils.randomAlphabetic(4)}"
            testGitService.createNewBranchAndSwitch(newBranch, testFolder)
        and: "committed file"
            File fileToCommit =
                    new File(testFolder.getPath() + "/file-" + randomAlphabetic(5)).with(true)
                            { it.createNewFile() }
            gitService.commitChanges(new FileChanges([new FileCreated(fileToCommit)]))

        when: "repository is cloned"
            testGitService.cloneRepository(GIT_REMOTE, clonedTestFolder, newBranch)
        then: "file should be present in cloned repository"
            clonedTestFolder.listFiles()[0].listFiles().any { it.name == fileToCommit.name }
    }

    def "Repository should be pulled"() {
        given: "repository with random branch"
            def gitService = createRepository()
            def newBranch = "branch_${RandomStringUtils.randomAlphabetic(4)}"
            testGitService.createNewBranchAndSwitch(newBranch, testFolder)
        and: "committed file"
            File fileToCommit = new File(testFolder.getPath() + "/file-" + randomAlphabetic(5)).with(true)
                    { it.createNewFile() }
            gitService.commitChanges(new FileChanges([new FileCreated(fileToCommit)]))
        and: "cloned repository"
            testGitService.cloneRepository(GIT_REMOTE, clonedTestFolder, newBranch)
            def testRepoInClonedTestFolder = new File("${clonedTestFolder.toPath()}/test_repository")

        when: "committed file is changed"
            fileToCommit.append(RANDOM_TEXT)
        and: "file is committed"
            gitService.commitChanges(new FileChanges([new FileModified(fileToCommit)]))
        and: "cloned repository is pulled"
            testGitService.pull(testRepoInClonedTestFolder)
        then: "changed file is present"
            new File("${testRepoInClonedTestFolder.getAbsolutePath()}/${fileToCommit.getName()}").getText() == RANDOM_TEXT
    }

    def "Should be able to create new branch and switch to it"() {
        given: "initialized empty git repo"
            new GitService(testFolder.getPath(), GIT_REMOTE, gitServerNetwork).createRepository()

        when: "new branch is created"
            def newBranch = "newBranch"
            testGitService.createNewBranchAndSwitch(newBranch)
        then: "it should be visible"
            processExecutor.execute(["git", "branch", "--show-current"], "show current branch")
                    .result()
                    .replace("\n", "").endsWith(newBranch)
    }

    def createRepository() {
        def gitService = new GitService(testFolder.getPath(), GIT_REMOTE, gitServerNetwork)
        gitService.createRepository()

        return gitService
    }
}