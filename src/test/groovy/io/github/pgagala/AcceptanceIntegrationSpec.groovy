package io.github.pgagala

import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.github.pgagala.util.TestGitService
import org.apache.commons.io.FileUtils
import spock.lang.Timeout
import spock.util.concurrent.PollingConditions
import sun.security.action.GetPropertyAction

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

import static org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils.randomAlphabetic

@Timeout(value = 2, unit = TimeUnit.MINUTES)
@SuppressWarnings("GroovyAccessibility")
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

    File folderWithDuplicates
    File folderWithDuplicatesFolder
    File folderWithDuplicatesFolderFile

    File synchronizedRepoFolder
    File clonedTestRepoFolder

    def setup() {
        def testFolderName = "test_folder_" + randomAlphabetic(4)
        testFolder = Files.createTempDirectory(testFolderName).toFile().with(true) { it.createNewFile() }
        folder1 = new File("$testFolder.path/folder1").with(true) { it.mkdir() }
        folder1File = new File("$testFolder.path/folder1/file1.1").with(true) { it.createNewFile() }
        folder1Folder = new File("$testFolder.path/folder1/folder").with(true) { it.mkdir() }
        folder1FolderFile1 = new File("$testFolder.path/folder1/folder/file1.2").with(true) { it.createNewFile() }
        folder1FolderFile2 = new File("$testFolder.path/folder1/folder/file1.3").with(true) { it.createNewFile() }

        folder2 = new File("$testFolder.path/folder2").with(true) { it.mkdir() }
        folder2File = new File("$testFolder.path/folder2/file2.1").with(true) { it.createNewFile() }
        folder2Folder = new File("$testFolder.path/folder2/folder").with(true) { it.mkdir() }
        folder2FolderFile1 = new File("$testFolder.path/folder2/folder/file2.2").with(true) { it.createNewFile() }
        folder2FolderFile2 = new File("$testFolder.path/folder2/folder/file2.3").with(true) { it.createNewFile() }

        folderWithDuplicates = new File("$testFolder.path/folderWithDuplicates").with(true) { it.mkdir() }
        folderWithDuplicatesFolder = new File("$testFolder.path/folderWithDuplicates/folder").with(true) { it.mkdir() }
        folderWithDuplicatesFolderFile = new File("$testFolder.path/folderWithDuplicates/folder/file").with(true) { it.mkdir() }

        def synchronizedRepoFolderName = "test_repo_${randomAlphabetic(4)}"
        synchronizedRepoFolder = new File("${Path.of(GetPropertyAction.privilegedGetProperty("java.io.tmpdir"))}/$synchronizedRepoFolderName")

        def clonedTestRepoFolderName = "cloned_test_repo_${randomAlphabetic(4)}"
        clonedTestRepoFolder = Files.createTempDirectory(clonedTestRepoFolderName).toFile()
        testGitService = new TestGitService(new GitRepositoryLocal(clonedTestRepoFolder), gitServerNetwork)
    }

    def cleanup() {
        if (testFolder.exists()) {
            FileUtils.forceDelete(testFolder)
        }
        if (synchronizedRepoFolder.exists()) {
            FileUtils.forceDelete(synchronizedRepoFolder)
        }
        if (clonedTestRepoFolder.exists()) {
            FileUtils.forceDelete(clonedTestRepoFolder)
        }
    }

    def "acceptance test"() {
        given: "randomized branch"
            def newBranch = new GitBranch("branch_${randomAlphabetic(4)}")

        expect: "2 folders with files exist"
            filesAmount(testFolder) == 13

        when: "synchronizer is started (watching 2 folders)"
            ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("acceptance-test-%d").build())

            CompletableFuture<Boolean> appStarted = new CompletableFuture<>()
            executor.submit(() -> {
                GitSynchronizerApplication.main("-p", "$folder1.path,$folder2.path", "-g", GIT_REMOTE.value, "-b", newBranch.value, "-r", synchronizedRepoFolder.getAbsolutePath(),
                        "-n", gitServerNetwork)
                appStarted.complete(true)
                return appStarted
            })

        then: "app started"
            new PollingConditions(timeout: 15).eventually {
                appStarted.isDone() && !appStarted.isCompletedExceptionally()
            }
        and: "all files are copied to local synchronized repository folder"
            new PollingConditions(timeout: 15).eventually {
                assert filesAmount(synchronizedRepoFolder, "\\.git") == 6
            }

        when: "folder1File is edited"
            folder1File.append("bla")
        then: "changed file is copied to synchronized repository"
            new PollingConditions(timeout: 15).eventually {
                new File("$synchronizedRepoFolder/$folder1File.name").text.endsWith("bla")
            }

//        and: "should be visible in cloned repository"
//            testGitService.cloneRepository(GIT_REMOTE, new GitRepositoryLocal(clonedTestRepoFolder), newBranch)
//            clonedTestRepoFolder.listFiles()[0].listFiles().any { it.name == folder1File.name }


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

    static def filesAmount(File file, String excludedPattern = null) {
        assert file.exists()
        return new FileNameByRegexFinder()
                .getFileNames(file.getAbsolutePath(), ".", excludedPattern)
                .size()
    }

}