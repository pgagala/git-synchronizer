package io.github.pgagala


import org.mockito.Mockito
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.nio.file.Path

import static io.github.pgagala.util.SpockMockitoVerifier.toSpockVerification

class FileSynchronizerSpec extends Specification implements FileChangesSampleData {

    FileSynchronizer fileSynchronizer

    def "new files should be copied to synchronized folder and added to git repo"() {
        given: "file watcher with recorded files changes"
            FileWatcher fileWatcher = Mock(FileWatcher) {
                occurredFileChanges() >>> [filesChanges, new FileChanges([])]
            }
        and: "file synchronizer with mocked git service"
            GitService gitService = Mockito.mock(GitService)
            FileManager fileManager = Mockito.mock(FileManager)
            fileSynchronizer = new FileSynchronizer(fileWatcher, gitService, fileManager)

        when: "synchronizer is started"
            fileSynchronizer.run()
        then: "new files should be copied to synchronized folder"
            new PollingConditions(timeout: 2).eventually {
                toSpockVerification(Mockito.verify(fileManager).copy(filesChanges.collect {it.file()}))
            }
        and: "committed to git repository"
            new PollingConditions(timeout: 2).eventually {
                toSpockVerification(Mockito.verify(gitService).commitChanges(filesChanges))
            }

        where:
            filesChanges << [
                    fileChanges([fileCreated(file("file1")), fileCreated(file("file2"))]),
                    fileChanges([fileCreated(file("file3")), fileModified(file("file2"))]),
                    fileChanges([fileModified(file("file4"))])
            ]
    }

    def "deleted file should be deleted from synchronized folder and added to git repo"() {
        given: "files changes"
            def filesChanges = fileChanges([fileCreated(file("file1")), fileDeleted(file("file1", false))])
        and: "file watcher with recorded files changes"
            FileWatcher fileWatcher = Mock(FileWatcher) {
                occurredFileChanges() >>> [filesChanges, new FileChanges([])]
            }
        and: "file synchronizer with mocked git service"
            GitService gitService = Mockito.mock(GitService)
            FileManager fileManager = Mockito.mock(FileManager)
            fileSynchronizer = new FileSynchronizer(fileWatcher, gitService, fileManager)

        when: "synchronizer is started"
            fileSynchronizer.run()
        then: "new file should be copied to synchronized folder"
            new PollingConditions(timeout: 2).eventually {
                toSpockVerification(Mockito.verify(fileManager).copy(filesChanges.newOrModifiedFiles()))
            }
        and: "new file should be deleted from synchronized folder"
            new PollingConditions(timeout: 2).eventually {
                toSpockVerification(Mockito.verify(fileManager).deleteFromTargetPath(filesChanges.deletedFiles().collect {it.name}))
            }
        and: "committed to git repository"
            new PollingConditions(timeout: 2).eventually {
                toSpockVerification(Mockito.verify(gitService).commitChanges(filesChanges))
            }
    }

    def file(String name, boolean exists = true) {
        return Mock(File) {
            isFile() >> exists
            exists() >> exists
            getName() >> name
            toPath() >> Path.of("/")
        }
    }
}
