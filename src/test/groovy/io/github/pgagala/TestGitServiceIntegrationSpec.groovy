package io.github.pgagala


import io.github.pgagala.util.TestGitService
import org.apache.commons.io.FileUtils
import spock.lang.Timeout

import java.nio.file.Files
import java.util.concurrent.TimeUnit

import static org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils.randomAlphabetic

@SuppressWarnings("GroovyAccessibility")
@Timeout(value = 2, unit = TimeUnit.MINUTES)
class TestGitServiceIntegrationSpec extends IntegrationSpec {

    public static final def GIT_REMOTE = new GitServerRemote("http://$gitServerIp/test_repository.git")
    public static final String RANDOM_TEXT = "lorem ipsum-${randomAlphabetic(12)}"

    GitRepositoryLocal gitLocal
    GitRepositoryLocal clonedLocalGit
    TestGitService testGitService
    FileManager fileManager
    ProcessExecutor processExecutor

    def setup() {
        def testRepoFolderName = "test_repo_" + randomAlphabetic(4)
        def clonedTestRepoFolderName = "cloned_test_repo_" + randomAlphabetic(4)

        gitLocal = new GitRepositoryLocal(Files.createTempDirectory(testRepoFolderName).toFile())
        clonedLocalGit = new GitRepositoryLocal(Files.createTempDirectory(clonedTestRepoFolderName).toFile())
        testGitService = new TestGitService(gitLocal, gitServerNetwork)

        fileManager = new FileManager(testRepoFolderName)
        processExecutor = new ProcessExecutor(gitLocal.value)
    }

    def cleanup() {
        if (gitLocal.value.exists()) {
            FileUtils.forceDelete(gitLocal.value)
        }
    }

    def "Repository should be cloned"() {
        given: "repository with random branch"
            def newBranch = new GitBranch("branch_${randomAlphabetic(4)}")
            def gitService = createRepository(newBranch)
        and: "committed file"
            File fileToCommit =
                    new File(gitLocal.value.getPath() + "/file-" + randomAlphabetic(5)).with(true)
                            { it.createNewFile() }
            gitService.commitChanges(new FileChanges([FileCreated.of(fileToCommit)]))

        when: "repository is cloned"
            testGitService.cloneRepository(GIT_REMOTE, clonedLocalGit, newBranch)
        then: "file should be present in cloned repository"
            clonedLocalGit.value.listFiles()[0].listFiles().any { it.name == fileToCommit.name }
    }

    def "Repository should be pulled"() {
        given: "repository with random branch"
            def newBranch = new GitBranch("branch_${randomAlphabetic(4)}")
            def gitService = createRepository(newBranch)
        and: "committed file"
            File fileToCommit = new File(gitLocal.value.getPath() + "/file-" + randomAlphabetic(5)).with(true)
                    { it.createNewFile() }
            gitService.commitChanges(new FileChanges([FileCreated.of(fileToCommit)]))
        and: "cloned repository"
            testGitService.cloneRepository(GIT_REMOTE, clonedLocalGit, newBranch)
            def testRepoInClonedTestFolder = new File("${clonedLocalGit.value.toPath()}/test_repository")

        when: "committed file is changed"
            fileToCommit.append(RANDOM_TEXT)
        and: "file is committed"
            gitService.commitChanges(new FileChanges([FileModified.of(fileToCommit)]))
        and: "cloned repository is pulled"
            testGitService.pull(testRepoInClonedTestFolder)
        then: "changed file is present"
            new File("${testRepoInClonedTestFolder.getAbsolutePath()}/${fileToCommit.getName()}").getText() == RANDOM_TEXT
    }

    def createRepository(GitBranch gitBranch) {
        def gitService = new GitService(GIT_REMOTE, gitLocal, gitBranch, gitServerNetwork)
        gitService.createRepository()

        return gitService
    }
}