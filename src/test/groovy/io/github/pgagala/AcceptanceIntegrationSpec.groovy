package io.github.pgagala

import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.github.pgagala.util.TestGitService
import org.apache.commons.io.FileUtils
import spock.util.concurrent.PollingConditions
import sun.security.action.GetPropertyAction

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import static org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils.randomAlphabetic

class AcceptanceIntegrationSpec extends IntegrationSpec {

    public static final GitServerRemote GIT_REMOTE = new GitServerRemote("http://$gitServerIp/test_repository.git")

    TestGitService testGitService

    File testFolder
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

    File testRepoFolder
    File clonedTestRepoFolder

    def setup() {
        def testFolderName = "test_folder_" + randomAlphabetic(4)
        testFolder = Files.createTempDirectory(testFolderName).toFile().with(true) { it.createNewFile() }
        folder1 = new File("$testFolder.path/folder1").with(true) { it.mkdir() }
        folder1File = new File("$testFolder.path/folder1/file").with(true) { it.createNewFile() }
        folder1Folder = new File("$testFolder.path/folder1/folder").with(true) { it.mkdir() }
        folder1FolderFile1 = new File("$testFolder.path/folder1/folder/file").with(true) { it.createNewFile() }
        folder1FolderFile2 = new File("$testFolder.path/folder1/folder/file2").with(true) { it.createNewFile() }
        folder2 = new File("$testFolder.path/folder2").with(true) { it.mkdir() }
        folder2File = new File("$testFolder.path/folder2/file").with(true) { it.createNewFile() }
        folder2Folder = new File("$testFolder.path/folder2/folder").with(true) { it.mkdir() }
        folder2FolderFile1 = new File("$testFolder.path/folder2/folder/file").with(true) { it.createNewFile() }
        folder2FolderFile2 = new File("$testFolder.path/folder2/folder/file2").with(true) { it.createNewFile() }

        def testRepoFolderName = "test_repo_${randomAlphabetic(4)}"
        testRepoFolder = new File("${Path.of(GetPropertyAction.privilegedGetProperty("java.io.tmpdir"))}/$testRepoFolderName")

        def clonedTestRepoFolderName = "cloned_test_repo_${randomAlphabetic(4)}"
        clonedTestRepoFolder = Files.createTempDirectory(clonedTestRepoFolderName).toFile()
        testGitService = new TestGitService(new GitRepositoryLocal(clonedTestRepoFolder), gitServerNetwork)
    }

    def cleanup() {
        if (testFolder.exists()) {
            FileUtils.forceDelete(testFolder)
        }
        if (testRepoFolder.exists()) {
            FileUtils.forceDelete(testRepoFolder)
        }
        if (clonedTestRepoFolder.exists()) {
            FileUtils.forceDelete(clonedTestRepoFolder)
        }
    }

    def filesAmount(File file, String excludedPattern = null) {
        assert file.exists()
        return new FileNameByRegexFinder()
                .getFileNames(file.getAbsolutePath(), ".", excludedPattern)
                .size()

//        def files = []
//        file.eachFileRecurse { files += it }
//        return files.size()
    }

    def "acceptance test"() {
        given: "randomized branch"
            def newBranch = "branch_${randomAlphabetic(4)}"

        expect: "2 folders with files exist"
            filesAmount(testFolder) == 10

        when: "synchronizer is started (watching 2 folders)"
            ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("acceptance-test-%d").build())
        GitSynchronizerApplication.main("-p", "$folder1.path,$folder2.path", "-g", GIT_REMOTE.value, "-b", newBranch, "-r", testRepoFolder.getAbsolutePath())
//            Future<Boolean> job = executor.submit(() -> {
//                GitSynchronizerApplication.main("-p", "$folder1.path,$folder2.path", "-g", GIT_REMOTE.value, "-b", newBranch, "-r", testRepoFolder.getAbsolutePath())
//            }, true)
//        sleep(3000)
//            job.cancel(true)
//        executor.shutdown()
//        sleep(222000)
//                    job.cancel(true)
//        executor.shutdownNow()
//            job.cancel(true)

        then: "all files are copied to local synchronized repository folder"
            new PollingConditions(timeout: 2).eventually {
                assert filesAmount(testRepoFolder, "\\.git") == 10
            }

//
//        and: "folder1File is edited"
//            folder1File.append("bla")
//        then: "change is committed to repository"
//        and: "should be visible in cloned repository"
//            testGitService.cloneRepository(GIT_REMOTE, new GitRepositoryLocal(clonedTestRepoFolder), newBranch)
//            clonedTestFolder.listFiles()[0].listFiles().any { it.name == folder1File.name }


//        when: "folder1FolderFile1 is edited"
//
//        then: "change is committed to repository"
//
//        when: "folder1FolderFile1 is removed"
//
//        then: "change is committed to repository"
//
//        when: "folder1FolderFile3 is added"
//
//        then: "change is committed to repository"
//
//
//        when: "application is shutted down"
//
//        then: "local synchronized repo is deleted"
    }

}