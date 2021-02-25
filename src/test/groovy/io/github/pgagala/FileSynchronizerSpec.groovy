package io.github.pgagala

import org.mockito.Mockito
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static SpockMockitoVerifier.toSpockVerification

class FileSynchronizerSpec extends Specification implements FileWatcherSampleData {

    FileSynchronizer fileSynchronizer

    def "new files should be added to git repo"() {
        given: "file watcher with recorded files changes"
            FileWatcher fileWatcher = Mock(FileWatcher) {
                occurredFileChanges() >>> [filesChanges, new FileChanges([])]
            }
        and: "file synchronizer with mocked git service"
            GitService gitService = Mockito.mock(GitService)
            fileSynchronizer = new FileSynchronizer(fileWatcher, gitService)

        when: "synchronizer is started"
            fileSynchronizer.run()
        then: "new files should be committed to git repository"
            new PollingConditions(timeout: 2).eventually {
                toSpockVerification(Mockito.verify(gitService).commitChanges(filesChanges))
            }

        where:
            filesChanges << [
                    fileChanges([fileCreated(new File("file1")), fileCreated(new File("file2"))]),
                    fileChanges([fileDeleted(new File("file1"))]),
                    fileChanges([fileCreated(new File("file3")), fileModified(new File("file2"))])
            ]
    }
}
