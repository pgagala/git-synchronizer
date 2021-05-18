package io.github.pgagala.gitsynchronizer

import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.github.pgagala.gitsynchronizer.util.TestGitService
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

@Timeout(value = 5, unit = TimeUnit.MINUTES)
@SuppressWarnings(["GroovyAccessibility", "GroovyAssignabilityCheck"])
class AcceptanceIntegrationSpec extends IntegrationSpec {

    public static final int WAITING_IN_SEC = 30
    public static final GitServerRemote GIT_REMOTE = new GitServerRemote("http://$gitServerIp/test_repository.git")
    public static final String BAR = "bar"
    public static final String FOO = "foo"
    public static final String BLA = "bla"
    public static final String TRA = "tra"

    File testFolder
    File folder1
    File folder1File0
    File folder1File1
    File folder1Folder
    File folder1FolderFileSwap
    File folder1FolderFile2
    File folder1FolderFile3
    File folder1FolderFile4
    File folder2
    File folder2File
    File folder2Folder
    File folder2FolderFile2
    File folder2FolderFile3

    File folderWithDuplicates
    File folderWithDuplicatesFile

    File synchronizedRepoFolder
    File anotherSynchronizedRepoFolder
    File anotherSynchronizedRepoFolder2

    TestGitService testGitService

    @SuppressWarnings('unused')
    def setup() {
        def testFolderName = "test_folder_" + randomAlphabetic(4)
        testFolder = Files.createTempDirectory(testFolderName).toFile().with(true) { it.createNewFile() }

        folder1 = new File("$testFolder.path/folder1").with(true) { it.mkdir() }
        folder1File0 = new File("$testFolder.path/folder1/file0").with(true) { it.createNewFile() }
        folder1File1 = new File("$testFolder.path/folder1/file1.1").with(true) { it.createNewFile() }
        folder1Folder = new File("$testFolder.path/folder1/folder").with(true) { it.mkdir() }
        folder1FolderFileSwap = new File("$testFolder.path/folder1/folder/.file.swp").with(true) { it.createNewFile() }
        folder1FolderFile2 = new File("$testFolder.path/folder1/folder/file1.2").with(true) { it.createNewFile() }
        folder1FolderFile3 = new File("$testFolder.path/folder1/folder/file1.3").with(true) { it.createNewFile() }
        folder1FolderFile4 = new File("$testFolder.path/folder1/folder/file1.4")

        folder2 = new File("$testFolder.path/folder2").with(true) { it.mkdir() }
        folder2File = new File("$testFolder.path/folder2/file2.1").with(true) { it.createNewFile() }
        folder2Folder = new File("$testFolder.path/folder2/folder").with(true) { it.mkdir() }
        folder2FolderFile2 = new File("$testFolder.path/folder2/folder/file2.2").with(true) { it.createNewFile() }
        folder2FolderFile3 = new File("$testFolder.path/folder2/folder/file2.3").with(true) { it.createNewFile() }

        folderWithDuplicates = new File("$testFolder.path/folderWithDuplicates").with(true) { it.mkdir() }
        folderWithDuplicatesFile = new File("$testFolder.path/folderWithDuplicates/file1.1").with(true) { it.createNewFile() }

        def synchronizedRepoFolderName = "test_repo_${randomAlphabetic(4)}"
        synchronizedRepoFolder = new File("${Path.of(GetPropertyAction.privilegedGetProperty("java.io.tmpdir"))}/$synchronizedRepoFolderName")

        def clonedTestRepoFolderName = "another_test_repo_${randomAlphabetic(4)}"
        def clonedTestRepoFolderName2 = "another_test_repo2_${randomAlphabetic(4)}"
        anotherSynchronizedRepoFolder = Files.createTempDirectory(clonedTestRepoFolderName).toFile()
        anotherSynchronizedRepoFolder2 = Files.createTempDirectory(clonedTestRepoFolderName2).toFile()

        testGitService = new TestGitService(new GitRepositoryLocal(synchronizedRepoFolder), gitServerNetwork)
    }

    @SuppressWarnings('unused')
    def cleanup() {
        try {
            if (testFolder.exists()) {
                FileUtils.forceDelete(testFolder)
            }
            if (synchronizedRepoFolder.exists()) {
                FileUtils.forceDelete(synchronizedRepoFolder)
            }
            if (anotherSynchronizedRepoFolder.exists()) {
                FileUtils.forceDelete(anotherSynchronizedRepoFolder)
            }
        }
        catch (Throwable ignored){}
    }

    def "acceptance test"() {
        given: "randomized branch"
            def newBranch = new GitBranch("branch_${randomAlphabetic(4)}")

        and: "executor"
            ExecutorService executor = Executors.newFixedThreadPool(3, new ThreadFactoryBuilder().setNameFormat("acceptance-test-%d").build())

        expect: "3 folders with files exist"
            filesAmount(testFolder) == 14

        when: "synchronizer is started"
            CompletableFuture<Boolean> appStarted =
                    starApplication(executor, newBranch,
                            "$folder1File1.path,$folder1Folder.path,$folder2File.path,$folder2Folder.path",
                            synchronizedRepoFolder)
        then: "app started"
            new PollingConditions(timeout: WAITING_IN_SEC, delay: 0.5).eventually {
                appStarted.isDone() && !appStarted.isCompletedExceptionally()
            }
        and: "all files from watched folders are copied to local synchronized repository folder"
            new PollingConditions(timeout: WAITING_IN_SEC, delay: 0.5).eventually {
                assert filesAmount(synchronizedRepoFolder, "\\.git") == 6
            }

        when: "folder1File0 is edited"
            folder1File0.append(TRA)
        and: "folder1File is edited"
            folder1File1.append(BLA)
        then: "watched file is copied to synchronized repository"
            new PollingConditions(timeout: WAITING_IN_SEC, delay: 0.5).eventually {
                new File("$synchronizedRepoFolder/$folder1File1.name").text.endsWith(BLA)
            }
        and: "non watched file is not copied"
            !new File("$synchronizedRepoFolder/$folder1File0.name").exists()

        when: "folder1FolderFile2 is edited"
            folder1FolderFile2.append(FOO)
        then: "changed file is copied to synchronized repository"
            new PollingConditions(timeout: WAITING_IN_SEC, delay: 0.5).eventually {
                new File("$synchronizedRepoFolder/$folder1FolderFile2.name").text.endsWith(FOO)
            }

        when: "folder1FolderFile2 is removed"
            folder1FolderFile2.delete()
        then: "file is deleted from synchronized repository. Last change is visible in git log"
            new PollingConditions(timeout: WAITING_IN_SEC, delay: 0.5).eventually {
                !new File("$synchronizedRepoFolder/$folder1FolderFile2.name").exists() &&
                        testGitService.log().result()
                                .contains("File deleted: $folder1FolderFile2")
            }

        when: "folder1FolderFile4 is added"
            folder1FolderFile4.createNewFile()
        then: "changed file is copied to synchronized repository"
            new PollingConditions(timeout: WAITING_IN_SEC, delay: 0.5).eventually {
                new File("$synchronizedRepoFolder/$folder1FolderFile4.name").exists()
            }

        when: "folder2File is edited"
            folder2File.append(BAR)
        then: "changed file is copied to synchronized repository"
            new PollingConditions(timeout: WAITING_IN_SEC, delay: 0.5).eventually {
                new File("$synchronizedRepoFolder/$folder2File.name").text.endsWith(BAR)
            }

        when: "folder2FolderFile3 is removed"
            folder2FolderFile3.delete()
        then: "file is deleted from synchronized repository. Last change is visible in git log"
            new PollingConditions(timeout: WAITING_IN_SEC, delay: 0.5).eventually {
                !new File("$synchronizedRepoFolder/$folder2FolderFile3.name").exists() &&
                        testGitService.log().result()
                                .contains("File deleted: $folder2FolderFile3")
            }

        when: "app is started with another synchronized repository"
            CompletableFuture<Boolean> appStarted2 =
                    starApplication(executor, newBranch, "$folder1.path", anotherSynchronizedRepoFolder)
        then: "app started"
            new PollingConditions(timeout: WAITING_IN_SEC, delay: 0.5).eventually {
                appStarted2.isDone() && !appStarted2.isCompletedExceptionally()
            }
        and: "another synchronized repository should has same files as synchronized repository"
            new PollingConditions(timeout: WAITING_IN_SEC, delay: 0.5).eventually {
                filesAmount(anotherSynchronizedRepoFolder, "\\.git") == 6
            }
            new File("$anotherSynchronizedRepoFolder/$folder1File0.name").text.endsWith(TRA)
            new File("$anotherSynchronizedRepoFolder/$folder1File1.name").text.endsWith(BLA)
            new File("$anotherSynchronizedRepoFolder/$folder1FolderFile3.name").exists()
            new File("$anotherSynchronizedRepoFolder/$folder1FolderFile4.name").exists()
            new File("$anotherSynchronizedRepoFolder/$folder2File.name").text.endsWith(BAR)
            new File("$anotherSynchronizedRepoFolder/$folder2FolderFile2.name").exists()

        when: "app is started watching paths which contains files with duplicated filename"
            CompletableFuture<Boolean> appStarted3 =
                    starApplication(executor, newBranch, "$folder1.path,$folderWithDuplicates.path", anotherSynchronizedRepoFolder2)
        then: "app shouldn't start"
            new PollingConditions(timeout: WAITING_IN_SEC, delay: 0.5).eventually {
                appStarted3.isCompletedExceptionally()
            }
    }

    private static CompletableFuture<Boolean> starApplication(ExecutorService executor, newBranch, String path, File synchronizedRepoFolder) {
        CompletableFuture<Boolean> appStarted = new CompletableFuture<>()
        executor.submit(() -> {
            try {
                GitSynchronizerApplication.main(
                        "-p", path,
                        "-g", GIT_REMOTE.value,
                        "-b", newBranch.value,
                        "-r", synchronizedRepoFolder.getAbsolutePath(),
                        "-n", gitServerNetwork)
                appStarted.complete(true)
            }
            catch (Throwable t) {
                appStarted.completeExceptionally(t)
            }
            return appStarted
        })
        return appStarted
    }

    static def filesAmount(File file, String excludedPattern = null) {
        assert file.exists()
        return new FileNameByRegexFinder()
                .getFileNames(file.getAbsolutePath(), ".", excludedPattern)
                .size()
    }
}