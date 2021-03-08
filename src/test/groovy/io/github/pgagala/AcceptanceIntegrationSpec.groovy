package io.github.pgagala

import io.github.pgagala.util.TestGitService
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils
import spock.lang.Ignore

import java.nio.file.Files

import static org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils.randomAlphabetic

@Ignore
class AcceptanceIntegrationSpec extends IntegrationSpec {

    public static final String GIT_REMOTE = "http://$gitServerIp/test_repository.git"

    TestGitService testGitService
    File testFolder
    File clonedTestFolder

    File folder1
    File folder1File
    File folder1Folder
    File folder1FolderFile1
    File folder1FolderFile2

    File folder2
    File folder2File
    File folder2Folder
    File folder2FolderFile1
    File folder2FolderFile2


    def setup() {
        def testFolderName = "test_folder_" + randomAlphabetic(4)
        testFolder = Files.createTempDirectory(testFolderName).toFile().with(true) {it.createNewFile()}
        folder1 = new File("$testFolder.path/folder1").with(true) {it.mkdir()}
        folder1File = new File("$testFolder.path/folder1/file").with(true) {it.createNewFile()}
        folder1Folder = new File("$testFolder.path/folder1/folder").with(true) {it.mkdir()}
        folder1FolderFile1 = new File("$testFolder.path/folder1/folder/file").with(true) {it.createNewFile()}
        folder1FolderFile2 = new File("$testFolder.path/folder1/folder/file2").with(true) {it.createNewFile()}
        folder2 = new File("$testFolder.path/folder2").with(true) {it.mkdir()}
        folder2File = new File("$testFolder.path/folder2/file").with(true) {it.createNewFile()}
        folder2Folder = new File("$testFolder.path/folder2/folder").with(true) {it.mkdir()}
        folder2FolderFile1 = new File("$testFolder.path/folder2/folder/file").with(true) {it.createNewFile()}
        folder2FolderFile2 = new File("$testFolder.path/folder2/folder/file2").with(true) {it.createNewFile()}

        def clonedTestRepoFolderName = "cloned_test_repo_" + randomAlphabetic(4)
        clonedTestFolder = Files.createTempDirectory(clonedTestRepoFolderName).toFile()
        testGitService = new TestGitService(clonedTestFolder, gitServerNetwork)
    }

    def "acceptance test"() {
        given: "randomized branch"
            def newBranch = "branch_${RandomStringUtils.randomAlphabetic(4)}"

        expect: "2 folders with files exist"
            assert testFolder.exists()
            def files = []
            testFolder.eachFileRecurse {files += it}
            files.size() == 10

        when: "synchronizer is started (watching 2 folders)"
            GitSynchronizerApplication.main("-p", "$folder1.path,$folder2.path", "-g", GIT_REMOTE, "-b", newBranch)
        and: "folder1File is edited"
            folder1File.append("bla")
        then: "change is committed to repository"
        and: "should be visible in cloned repository"
            testGitService.cloneRepository(GIT_REMOTE, clonedTestFolder, newBranch)
            clonedTestFolder.listFiles()[0].listFiles().any { it.name == folder1File.name }


        when: "folder1FolderFile1 is edited"

        then: "change is committed to repository"

        when: "folder1FolderFile1 is removed"

        then: "change is committed to repository"

        when: "folder1FolderFile3 is added"

        then: "change is committed to repository"



        when: "application is shutted down"

        then: "local synchronized repo is deleted"
    }

}