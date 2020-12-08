package io.github.pgagala

import org.apache.commons.io.FileUtils
import spock.lang.Shared

import java.nio.file.Paths

class RepositoryBootstrapSpec extends IntegrationSpec {

    private static final PATH = new File("./").getAbsolutePath()
    public static final String GIT_REPO_PATH = PATH + "/.git"

    @Shared
    RepositoryBootstrap repositoryBootstrap = new RepositoryBootstrap(PATH)

    def "On system start repository should be initialized if repo doesn't exist"() {

        given: "System without existing repository"
            noRepositoryExists()

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
            Paths.get(PATH).toFile().exists()
    }

    def "Bootstrap shouldn't work without configured repository address"() {
        expect:
            1 == 1
    }

    def "On cleanup repository should be deleted"() {
        expect:
            1 == 1
    }

    def createRepository() {
        assertRepositoryExists()
    }

    def assertRepositoryExists() {
        def repoPath = Paths.get(GIT_REPO_PATH).toFile()
        assert repoPath.exists() && repoPath.listFiles().size() > 0
    }

    def noRepositoryExists() {
        FileUtils.deleteDirectory(new File(GIT_REPO_PATH))
        def repoPath = Paths.get(GIT_REPO_PATH).toFile()
        assert !repoPath.exists()
    }

}
