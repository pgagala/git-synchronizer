package io.github.pgagala.gitsynchronizer


import org.apache.commons.io.FileUtils
import spock.lang.Timeout

import java.nio.file.Files
import java.util.concurrent.TimeUnit

import static org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils.randomAlphabetic

@SuppressWarnings("GroovyAccessibility")
@Timeout(value = 3, unit = TimeUnit.MINUTES)
class RepositoryBootstrapIntegrationSpec extends IntegrationSpec {

    File gitRepo
    File createdByItSpecFile
    RepositoryBootstrap repositoryBootstrap
    GitServerRemote gitRemote

    def setup() {
        gitRemote = new GitServerRemote("http://$gitServerIp/test_repository.git")
        gitRepo = Files.createTempDirectory("test-repo").toFile()
        createdByItSpecFile = new File(gitRepo.getAbsolutePath() + "/created-by-integration-spec")
        repositoryBootstrap = new RepositoryBootstrap(
                new GitService(gitRemote, new GitRepositoryLocal(gitRepo), GitBranch.DEFAULT_BRANCH, gitServerNetwork)
        )
    }

    def cleanup() {
        try {
            if (gitRepo.exists()) {
                FileUtils.forceDelete(gitRepo)
            }
        }
        catch (Throwable ignored){}
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

    def "On initialization repository should pull content from repo if any exists over there"() {
        given: "Already committed files on remote"
            def tempDir = File.createTempDir()
            GitService gitService = new GitService(gitRemote, new GitRepositoryLocal(tempDir), GitBranch.DEFAULT_BRANCH, gitServerNetwork)
            gitService.createRepository()
            def fileName = "/file-" + randomAlphabetic(5)
            File file = new File(tempDir.getPath() + fileName)
            assert !file.exists()
            file.createNewFile()
            assert gitService.commitChanges(new FileChanges([FileCreated.of(file)])).isSuccessful()

        when: "Bootstrap is initialized"
            repositoryBootstrap.initialize()
        then: "Local repository contains files existing on remote"
            gitRepo.listFiles().any {it.shallowEquals(file)}
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
        createdByItSpecFile = new File(gitRepo.getAbsolutePath() + "/created-by-integration-spec")
        createdByItSpecFile.createNewFile()
        assertRepositoryCreatedByItExists()
    }

    void assertRepositoryCreatedByBootstrapExists() {
        def gitRepositoryFiles = gitRepo.listFiles()
        assert gitRepositoryFiles.size() > 0 && !gitRepositoryFiles.contains(createdByItSpecFile)
    }

    void assertRepositoryCreatedByItExists() {
        assert gitRepo.listFiles().contains(createdByItSpecFile)
    }

    void assertRepositoryDoesntExist() {
        assert !gitRepo.exists()
    }

}
