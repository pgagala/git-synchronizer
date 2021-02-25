package io.github.pgagala

import org.mockito.Mockito
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static SpockMockitoVerifier.toSpockVerification

class FileSynchronizerSpec extends Specification implements FileWatcherSampleData {

    FileSynchronizer fileSynchronizer

    def "new files should be added to git repo"() {
        given: "file watcher with recorded new files"
            def fileChanges = fileChanges([fileCreated(new File("file1")), fileCreated(new File("file2"))])
            FileWatcher fileWatcher = Mock(FileWatcher) {
                occurredFileChanges() >>> [fileChanges, new FileChanges([])]
            }
        and: "file synchronizer with mocked git service"
            GitService gitService = Mockito.mock(GitService)
            fileSynchronizer = new FileSynchronizer(fileWatcher, gitService)

        when: "synchronizer is started"
            fileSynchronizer.run()
        then: "new files should be committed to git repository"
            new PollingConditions(timeout: 2).eventually {
                toSpockVerification(Mockito.verify(gitService).commitChanges(fileChanges))
            }

    }

    def "discarded files should be removed git repo"() {
        expect:
            1 == 1
    }

    def "changed files should be updated in git repo"() {
        expect:
            1 == 1
    }

}
