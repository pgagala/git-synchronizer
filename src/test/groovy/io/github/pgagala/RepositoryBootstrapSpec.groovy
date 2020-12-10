package io.github.pgagala

import org.apache.commons.io.FileUtils
import spock.lang.Shared

import java.nio.file.Paths

class RepositoryBootstrapSpec extends IntegrationSpec {

    private static final GIT_REPO_PATH = new File("./test-repo").getAbsolutePath()
    private static final String GIT_PATH = GIT_REPO_PATH + "/.git"
    private static final File CREATED_BY_INTEGRATION_SPEC = new File(GIT_PATH + "./created-by-integration-spec")

    @Shared
    RepositoryBootstrap repositoryBootstrap = new RepositoryBootstrap(GIT_REPO_PATH)

    def setup() {
        deleteRepositoryIfExists()
    }

    def "On system start repository should be initialized if repo doesn't exist"() {
        when: "Bootstrap is initialized"
            repositoryBootstrap.initialize()

        then: "Repository exists"
            assertRepositoryExists()
    }

    def "On system start repository should be discarded and initialized if repo exists"() {
        given: "Already existing repository with some content"
            createRepository()

        when:
            repositoryBootstrap.initialize()

        then:
            assertRepositoryExists()
    }

    def "On cleanup repository should be deleted"() {
        given: "Already existing repository with some content"
            createRepository()

        when: "Cleanup is invoked"
            repositoryBootstrap.cleanup()

        then:
            assertRepositoryDoesntExist()
    }

    def createRepository() {
        executeAndWaitUntilFinished()
        def initRepository = new ProcessBuilder().command("docker", "run", "--rm", "-v", "/home/pgagala/IdeaProjects/test:/git", "alpine/git", "init").start()
        executeAndWaitUntilFinished(initRepository)
        def addFileCreatedByIntSpec = new ProcessBuilder().command("docker", "run", "--rm", "-v", "/home/pgagala/IdeaProjects/test:/git", "alpine/git", "init").start()
        //add file created_by_integration_spec
        assertRepositoryCreatedByItExists()
    }

    def executeAndWaitUntilFinished(Process process) {
        process.waitForProcessOutput()
        assert process.exitValue() == 0
    }

    def deleteRepositoryIfExists() {
        FileUtils.deleteDirectory(new File(GIT_PATH))
        assertRepositoryDoesntExist()
    }

    def assertRepositoryExists() {
        def repoPath = Paths.get(GIT_PATH).toFile()
        assert repoPath.exists() && repoPath.listFiles().size() > 0 && !repoPath.listFiles().contains(CREATED_BY_INTEGRATION_SPEC)
    }

    def assertRepositoryCreatedByItExists() {
        def repoPath = Paths.get(GIT_PATH).toFile()
        assert repoPath.exists() && repoPath.listFiles().size() > 0 && repoPath.listFiles().contains(CREATED_BY_INTEGRATION_SPEC)
    }

    def assertRepositoryDoesntExist() {
        def repoPath = Paths.get(GIT_PATH).toFile()
        assert !repoPath.exists()
    }

}
