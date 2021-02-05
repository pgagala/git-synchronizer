package io.github.pgagala

import org.apache.commons.io.FileUtils
import spock.lang.Shared

import java.nio.file.Files

class RepositoryBootstrapIntegrationSpec extends IntegrationSpec {

    @Shared
    private File gitRepo

    @Shared
    private File gitFolderPath

    @Shared
    private File createdByItSpecFile

    @Shared
    RepositoryBootstrap repositoryBootstrap

    def setup() {
        gitRepo = Files.createTempDirectory("test-repo").toFile()
        gitFolderPath = new File(gitRepo.getAbsolutePath() + "/.git")
        createdByItSpecFile = new File(gitFolderPath.getAbsolutePath() + "/created-by-integration-spec")
        repositoryBootstrap = new RepositoryBootstrap(new GitService(gitRepo.getAbsolutePath(), ""))
    }

    def cleanup() {
        if (gitRepo.exists()) {
            FileUtils.forceDelete(gitRepo)
        }
    }

    def "On system start repository should be initialized if repo doesn't exist"() {
        when: "Bootstrap is initialized"
            repositoryBootstrap.initialize()

        then: "Repository exists"
            assertRepositoryCreatedByBootstrapExists()
    }

    def "On system start repository should be discarded and initialized if repo exists"() {
        given: "Already existing repository with some content"
            createRepository()

        when:
            repositoryBootstrap.initialize()

        then:
            assertRepositoryCreatedByBootstrapExists()
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
        Files.createDirectory(gitFolderPath.toPath())
        createdByItSpecFile.createNewFile()
        assertRepositoryCreatedByItExists()
    }

    void assertRepositoryCreatedByBootstrapExists() {
        def gitRepositoryFiles = gitFolderPath.listFiles()
        assert gitRepositoryFiles.size() > 0 && !gitRepositoryFiles.contains(createdByItSpecFile)
    }

    void assertRepositoryCreatedByItExists() {
        assert gitFolderPath.listFiles().contains(createdByItSpecFile)
    }

    void assertRepositoryDoesntExist() {
        assert !gitFolderPath.exists()
    }

}
