package io.github.pgagala

import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.nio.file.Path
import java.nio.file.WatchKey
import java.nio.file.WatchService

class FileWatcherSpec extends Specification implements FileWatcherSampleData {

    public static final File FILE1 = new File("file1")
    public static final File FILE2 = new File("file2")

    def "occurredEvents should returned events without duplication"() {
        given: "Watch service which returned particular events"
            WatchKey key = Mock(WatchKey) {
                pollEvents() >> events
            }
            WatchService watchService = Mock(WatchService) {
                take() >> key
            }
            FileWatcher fileWatcher = new FileWatcher(watchService, [Mock(Path)])

        when: "File watcher is started"
            fileWatcher.run()

        then: "Events without duplication in correct order are returned"
            new PollingConditions(timeout: 2).eventually {
                assert expectedFileChanges == fileWatcher.occurredFileChanges()
            }

        where:
            events                                                                           | expectedFileChanges
            [eventModify(FILE1), eventModify(FILE1)]                                         | fileChanges([fileModified(FILE1)])
            [eventCreate(FILE1), eventCreate(FILE1)]                                         | fileChanges([fileCreated(FILE1)])
            [eventDelete(FILE1), eventDelete(FILE1)]                                         | fileChanges([fileDeleted(FILE1)])
            [eventCreate(FILE1), eventDelete(FILE2), eventDelete(FILE2), eventModify(FILE1)] | fileChanges([fileCreated(FILE1), fileDeleted(FILE2), fileModified(FILE1)])
            [eventModify(FILE1), eventDelete(FILE2), eventCreate(FILE1), eventDelete(FILE2)] | fileChanges([fileModified(FILE1), fileDeleted(FILE2), fileCreated(FILE1)])
    }

    def "equal of file changes should work correctly"() {
        expect:
            (expectedFileChanges == fileChangesToCheck) == result

        where:
            expectedFileChanges                                    | fileChangesToCheck                                     || result
            fileChanges([])                                        | fileChanges([])                                        || true
            fileChanges([fileModified(FILE1)])                     | fileChanges([])                                        || false
            fileChanges([fileModified(FILE1)])                     | fileChanges([fileModified(FILE1)])                     || true
            fileChanges([fileDeleted(FILE1)])                      | fileChanges([fileDeleted(FILE1)])                      || true
            fileChanges([fileCreated(FILE1)])                      | fileChanges([fileCreated(FILE1)])                      || true
            fileChanges([fileModified(FILE1)])                     | fileChanges([fileDeleted(FILE1)])                      || false
            fileChanges([fileModified(FILE1)])                     | fileChanges([fileCreated(FILE1)])                      || false
            fileChanges([fileModified(FILE1)])                     | fileChanges([fileModified(FILE1), fileCreated(FILE1)]) || false
            fileChanges([fileModified(FILE1), fileCreated(FILE1)]) | fileChanges([fileModified(FILE1)])                     || false
            fileChanges([fileModified(FILE1), fileCreated(FILE1)]) | fileChanges([fileCreated(FILE1), fileModified(FILE1)]) || false
    }
}
