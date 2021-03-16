package io.github.pgagala

import org.apache.commons.io.FileUtils

import java.nio.file.Files

class RepositoryBootstrapIntegrationSpec extends IntegrationSpec {

    File gitRepo
    File gitFolderPath
    File createdByItSpecFile
    RepositoryBootstrap repositoryBootstrap

    def setup() {
        gitRepo = Files.createTempDirectory("test-repo").toFile()
        gitFolderPath = new File(gitRepo.getAbsolutePath() + "/.git")
        createdByItSpecFile = new File(gitFolderPath.getAbsolutePath() + "/created-by-integration-spec")
        repositoryBootstrap = new RepositoryBootstrap(
                new GitService(new GitServerRemote(""), new GitRepositoryLocal(gitRepo), GitService.DEFAULT_BRANCH)
        )
    }

    def cleanup() {
        if (gitRepo.exists()) {
            FileUtils.forceDelete(gitRepo)
        }
    }

    def "On initialization repository should be created if repo doesn't exist"() {
        when: "Bootstrap is initialized"
            repositoryBootstrap.initialize()

        then: "Repository exists"
            assertRepositoryCreatedByBootstrapExists()
    }

    def "On initialization repository should be discarded and created if repo exists"() {
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
