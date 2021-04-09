package io.github.pgagala

import com.google.common.util.concurrent.ThreadFactoryBuilder
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

@Timeout(value = 2222, unit = TimeUnit.MINUTES)
@SuppressWarnings(["GroovyAccessibility", "GroovyAssignabilityCheck"])
//@Ignore
class AcceptanceIntegrationSpec extends IntegrationSpec {

    public static final GitServerRemote GIT_REMOTE = new GitServerRemote("http://$gitServerIp/test_repository.git")

    File testFolder
    File folder1
    File folder1File
    File folder1Folder
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

    @SuppressWarnings('unused')
    def setup() {
        def testFolderName = "test_folder_" + randomAlphabetic(4)
        testFolder = Files.createTempDirectory(testFolderName).toFile().with(true) { it.createNewFile() }

        folder1 = new File("$testFolder.path/folder1").with(true) { it.mkdir() }
        folder1File = new File("$testFolder.path/folder1/file1.1").with(true) { it.createNewFile() }
        folder1Folder = new File("$testFolder.path/folder1/folder").with(true) { it.mkdir() }
        folder1FolderFile2 = new File("$testFolder.path/folder1/folder/file1.2").with(true) { it.createNewFile() }
        folder1FolderFile3 = new File("$testFolder.path/folder1/folder/file1.3").with(true) { it.createNewFile() }
        folder1FolderFile4 = new File("$testFolder.path/folder1/folder/file1.4")

        folder2 = new File("$testFolder.path/folder2").with(true) { it.mkdir() }
        folder2File = new File("$testFolder.path/folder2/file2.1").with(true) { it.createNewFile() }
        folder2Folder = new File("$testFolder.path/folder2/folder").with(true) { it.mkdir() }
        folder2FolderFile2 = new File("$testFolder.path/folder2/folder/file2.2").with(true) { it.createNewFile() }
        folder2FolderFile3 = new File("$testFolder.path/folder2/folder/file2.3").with(true) { it.createNewFile() }

        folderWithDuplicates = new File("$testFolder.path/folderWithDuplicates").with(true) { it.mkdir() }
        folderWithDuplicatesFile = new File("$testFolder.path/folderWithDuplicates/file1.1").with(true) { it.mkdir() }

        def synchronizedRepoFolderName = "test_repo_${randomAlphabetic(4)}"
        synchronizedRepoFolder = new File("${Path.of(GetPropertyAction.privilegedGetProperty("java.io.tmpdir"))}/$synchronizedRepoFolderName")

        def clonedTestRepoFolderName = "another_test_repo_${randomAlphabetic(4)}"
        def clonedTestRepoFolderName2 = "another_test_repo2_${randomAlphabetic(4)}"
        anotherSynchronizedRepoFolder = Files.createTempDirectory(clonedTestRepoFolderName).toFile()
        anotherSynchronizedRepoFolder2 = Files.createTempDirectory(clonedTestRepoFolderName2).toFile()
    }

    @SuppressWarnings('unused')
    def cleanup() {
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

    def "acceptance test"() {
        given: "randomized branch"
            def newBranch = new GitBranch("branch_${randomAlphabetic(4)}")

        and: "executor"
            ExecutorService executor = Executors.newFixedThreadPool(3, new ThreadFactoryBuilder().setNameFormat("acceptance-test-%d").build())

        expect: "3 folders with files exist"
            filesAmount(testFolder) == 12

        when: "synchronizer is started"
            CompletableFuture<Boolean> appStarted = new CompletableFuture<>()
            executor.submit(() -> {
                GitSynchronizerApplication.main(
                        "-p", "$folder1.path,$folder1Folder.path,$folder2.path,$folder2Folder.path",
                        "-g", GIT_REMOTE.value,
                        "-b", newBranch.value,
                        "-r", synchronizedRepoFolder.getAbsolutePath(),
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

        when: "folder1FolderFile2 is edited"
            folder1FolderFile2.append("foo")
        then: "changed file is copied to synchronized repository"
            new PollingConditions(timeout: 15).eventually {
                new File("$synchronizedRepoFolder/$folder1FolderFile2.name").text.endsWith("foo")
            }

        when: "folder1FolderFile2 is removed"
            folder1FolderFile2.delete()
        then: "file is deleted from synchronized repository"
            new PollingConditions(timeout: 15).eventually {
                !new File("$synchronizedRepoFolder/$folder1FolderFile2.name").exists()
            }

        when: "folder1FolderFile4 is added"
            folder1FolderFile4.createNewFile()
        then: "changed file is copied to synchronized repository"
            new PollingConditions(timeout: 15).eventually {
                new File("$synchronizedRepoFolder/$folder1FolderFile4.name").exists()
            }

        when: "folder2File is edited"
            folder2File.append("bar")
        then: "changed file is copied to synchronized repository"
            new PollingConditions(timeout: 15).eventually {
                new File("$synchronizedRepoFolder/$folder2File.name").text.endsWith("bar")
            }

        when: "folder2FolderFile3 is removed"
            folder2FolderFile3.delete()
        then: "file is deleted from synchronized repository"
            new PollingConditions(timeout: 15).eventually {
                !new File("$synchronizedRepoFolder/$folder2FolderFile3.name").exists()
            }

        when: "app is started with another synchronized repository"
            CompletableFuture<Boolean> appStarted2 = new CompletableFuture<>()
            executor.submit(() -> {
                GitSynchronizerApplication.main(
                        "-p", "$folder1.path",
                        "-g", GIT_REMOTE.value,
                        "-b", newBranch.value,
                        "-r", anotherSynchronizedRepoFolder.getAbsolutePath(),
                        "-n", gitServerNetwork)
                appStarted2.complete(true)
                return appStarted2
            })

        then: "app started"
            new PollingConditions(timeout: 15).eventually {
                appStarted2.isDone() && !appStarted2.isCompletedExceptionally()
            }
        and: "another synchronized repository should has same files as synchronized repository"
        //TODO add check in more details files
            assert filesAmount(anotherSynchronizedRepoFolder, "\\.git") == 5

//        when: "app is started watching paths which contains files with duplicated filename"
//            CompletableFuture<Boolean> appStarted3 = new CompletableFuture<>()
//            executor.submit(() -> {
//                GitSynchronizerApplication.main(
//                        "-p", "$folder1.path,$folderWithDuplicates.path",
//                        "-g", GIT_REMOTE.value,
//                        "-b", newBranch.value,
//                        "-r", anotherSynchronizedRepoFolder2.getAbsolutePath(),
//                        "-n", gitServerNetwork)
//                appStarted3.complete(true)
//                return appStarted3
//            })
//        then: "app shouldn't start"
//            new PollingConditions(timeout: 15).eventually {
//                appStarted3.isCompletedExceptionally()
//            }
    }

    static def filesAmount(File file, String excludedPattern = null) {
        assert file.exists()
        return new FileNameByRegexFinder()
                .getFileNames(file.getAbsolutePath(), ".", excludedPattern)
                .size()
    }
}