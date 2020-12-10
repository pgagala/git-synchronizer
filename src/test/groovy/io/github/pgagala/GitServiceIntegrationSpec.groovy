package io.github.pgagala

import org.apache.commons.io.FileUtils
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils
import spock.lang.Shared

import java.nio.file.Files

class GitServiceIntegrationSpec extends IntegrationSpec {

    @Shared
    File folderUnderRandomPath

    def "Git repository should be initialized and deleted"() {
        given: "Git service for random path"
            folderUnderRandomPath = Files.createTempDirectory("test_repo_" + RandomStringUtils.randomAlphabetic(4) + "_").toFile()
            FileUtils.forceDeleteOnExit(folderUnderRandomPath)
            GitService gitService = gitServiceForPath(folderUnderRandomPath.getPath())

        when: "Git is initialized"
            gitService.createRepository()

        then: "Git repository exists"
            folderUnderRandomPath.listFiles().any {it.getName() == '.git'}

        when: "Git repository is removed"
            gitService.deleteRepository()

        then: "Git repository doesn't exist"
            folderUnderRandomPath.listFiles().size() == 0
    }

    def gitServiceForPath(String path) {
        GitService gitService = new GitService(path)
        gitService.createRepository()

        return gitService
    }
}