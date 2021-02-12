package io.github.pgagala

import spock.lang.Specification


class FileSynchronizerSpec extends Specification implements FileWatcherSampleData {

    FileSynchronizer fileSynchronizer

    def "new files should be added to git repo"() {
        given: "file watcher with recorded new files"
            FileWatcher fileWatcher = Mock(FileWatcher) {
                occurredFileChanges() >> [fileCreated(new File("file1")), fileCreated(new File("file2"))]
            }
        fileSynchronizer = new FileSynchronizer(fileWatcher)

        when: "synchronizer is started"
            fileSynchronizer.run()

        then: "new files should be committed to git repository"

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
