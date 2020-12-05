package io.github.pgagala

import spock.lang.Specification
import spock.util.concurrent.PollingConditions
import sun.nio.fs.AbstractWatchKey

import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.WatchService

class FileWatcherSpec extends Specification {

    public static String PATH = "c:\\SomeFolder"
    public static final String FILE1 = "file1"
    public static final String FILE2 = "file2"

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
                assert eventsSame(flattenedEvents, fileWatcher.occurredEvents())
            }

        where:
            events                                                                           | flattenedEvents
            [eventModify(FILE1), eventModify(FILE1)]                                         | [eventModify(FILE1)]
            [eventCreate(FILE1), eventCreate(FILE1)]                                         | [eventCreate(FILE1)]
            [eventDelete(FILE1), eventDelete(FILE1)]                                         | [eventDelete(FILE1)]
            [eventCreate(FILE1), eventDelete(FILE2), eventDelete(FILE2), eventModify(FILE1)] | [eventCreate(FILE1), eventDelete(FILE2), eventModify(FILE1)]
            [eventModify(FILE1), eventDelete(FILE2), eventCreate(FILE1), eventDelete(FILE2)] | [eventCreate(FILE1), eventDelete(FILE2), eventModify(FILE1)]
    }

    def "eventsSame should return information about events equality"() {
        expect:
            eventsSame(expectedEvents, eventsToCheck) == result

        where:
            expectedEvents                           | eventsToCheck                            || result
            []                                       | []                                       || true
            [eventModify(FILE1)]                     | []                                       || false
            [eventModify(FILE1)]                     | [eventModify(FILE1)]                     || true
            [eventModify(FILE1)]                     | [eventDelete(FILE1)]                     || false
            [eventModify(FILE1)]                     | [eventCreate(FILE1)]                     || false
            [eventModify(FILE1)]                     | [eventModify(FILE1), eventCreate(FILE1)] || false
            [eventModify(FILE1), eventCreate(FILE1)] | [eventModify(FILE1)]                     || false
    }

    def eventModify(String file) {
        return new AbstractWatchKey.Event<Object>(StandardWatchEventKinds.ENTRY_MODIFY, Paths.get(PATH + file))
    }

    def eventCreate(String file) {
        return new AbstractWatchKey.Event<Object>(StandardWatchEventKinds.ENTRY_CREATE, Paths.get(PATH + file))
    }

    def eventDelete(String file) {
        return new AbstractWatchKey.Event<Object>(StandardWatchEventKinds.ENTRY_DELETE, Paths.get(PATH + file))
    }

    boolean eventsSame(List<WatchEvent<?>> expectedEvents1, List<WatchEvent<?>> eventsToCheck2) {
        if (expectedEvents1.size() != eventsToCheck2.size()) {
            return false
        }

        //TODO check in order
        expectedEvents1.stream()
                .allMatch({ ev1 ->
                    eventsToCheck2.stream().anyMatch({
                        ev2 -> ev2.context() == ev1.context() && ev2.kind() == ev1.kind()
                    })
                })
    }
}
