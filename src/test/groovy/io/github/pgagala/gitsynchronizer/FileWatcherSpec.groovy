package io.github.pgagala.gitsynchronizer

import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.nio.file.Path
import java.nio.file.WatchKey
import java.nio.file.WatchService

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY

class FileWatcherSpec extends Specification implements FileChangesSampleData {

    @Shared
    final File FILE1 = file("file1")

    @Shared
    final File FILE2 = file("file2")

    @Shared
    final File FILE3 = file("file3")

    @Shared
    final File FILE2_SWP = file(".file2.swp")

    @Shared
    final File FILE2_SWPX = file(".file2.swpx")

    WatchKey key
    WatchService watchService

    def setup() {
        key = Mock(WatchKey) {
            pollEvents() >> []
        }
        watchService = Mock(WatchService) { take() >> key }
    }

    def "On start files from watched paths should be added (without ignored files) as initialized events"() {
        given: "file watcher with paths"
            FileWatcher fileWatcher = new FileWatcher(watchService, [
                    Mock(Path) {
                        toFile() >> Mock(File) {
                            isFile() >> false
                        }
                        toString() >> "/"
                    }
            ], { f -> [FILE1, FILE2, FILE2_SWP, file(".~file3")] }, IgnoredFiles.intermediateIgnoredFiles())

        when: "file watcher is started"
            fileWatcher.run()
        then: "files situated under paths should be returned as created file events"
            FileChanges occurredFileChanges = fileWatcher.occurredFileChanges()
            occurredFileChanges == fileChanges([FileInitialized.of(FILE1), FileInitialized.of(FILE2)])
            occurredFileChanges.newOrModifiedFiles() == [FILE1, FILE2]
    }

    def "Exception should be thrown on file watcher start if there are any files in watched paths with same file name"() {
        when: "file watcher with duplicated files is created"
            new FileWatcher(watchService, [Mock(Path) {
                toFile() >> Mock(File) {
                    isFile() >> false
                }
            }], { f -> [FILE1, FILE1] }, IgnoredFiles.noIgnoredFiles())
        then: "exception should be thrown"
            thrown DuplicatedWatchedFileException
    }

    def "events correspond to ignored file patterns should be ignored during files watching"() {
        given: "Watch service which returned particular events"
            WatchKey key = Mock(WatchKey) {
                pollEvents() >> events
            }
            WatchService watchService = Mock(WatchService) {
                take() >> key
            }
            def watchedPaths =
                    [Mock(Path) {
                        toString() >> "/"
                        register(_ as WatchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE) >> key
                        toFile() >> Mock(File) {
                            isFile() >> false
                        }
                    }]
            FileWatcher fileWatcher = new FileWatcher(watchService, watchedPaths, { f -> [] }, IgnoredFiles.intermediateIgnoredFiles())

        when: "File watcher is started"
            fileWatcher.run()

        then: "Events without duplication in correct order are returned"
            new PollingConditions(timeout: 2).eventually {
                assert fileWatcher.occurredFileChanges() == expectedFileChanges
            }

        where:
            events                                                                                    | expectedFileChanges
            [eventModify(FILE1), eventModify(FILE1), eventCreate(FILE2_SWP)]                          | fileChanges([fileModified(FILE1)])
            [eventCreate(FILE1), eventCreate(FILE1), eventCreate(FILE2_SWP), eventModify(FILE2_SWPX)] | fileChanges([fileCreated(FILE1)])
    }

    def "occurredFileChanges should returned events without duplication"() {
        given: "Watch service which returned particular events"
            WatchKey key = Mock(WatchKey) {
                pollEvents() >> events
            }
            WatchService watchService = Mock(WatchService) {
                take() >> key
            }
            def watchedPaths =
                    [Mock(Path) {
                        toString() >> "/"
                        register(_ as WatchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE) >> key
                        toFile() >> Mock(File) {
                            isFile() >> false
                        }
                    }]
            FileWatcher fileWatcher = new FileWatcher(watchService, watchedPaths, { f -> [] }, IgnoredFiles.noIgnoredFiles())

        when: "File watcher is started"
            fileWatcher.run()

        then: "Events without duplication in correct order are returned"
            new PollingConditions(timeout: 2).eventually {
                assert fileWatcher.occurredFileChanges() == expectedFileChanges
            }

        where:
            events                                                                           | expectedFileChanges
            [eventModify(FILE1), eventModify(FILE1)]                                         | fileChanges([fileModified(FILE1)])
            [eventCreate(FILE1), eventCreate(FILE1)]                                         | fileChanges([fileCreated(FILE1)])
            [eventDelete(FILE1), eventDelete(FILE1)]                                         | fileChanges([fileDeleted(FILE1)])
            [eventCreate(FILE1), eventDelete(FILE2), eventDelete(FILE2), eventModify(FILE1)] | fileChanges([fileCreated(FILE1), fileDeleted(FILE2), fileModified(FILE1)])
            [eventModify(FILE1), eventDelete(FILE2), eventCreate(FILE1), eventDelete(FILE2)] | fileChanges([fileModified(FILE1), fileDeleted(FILE2), fileCreated(FILE1)])
            [eventCreate(FILE1), eventDelete(FILE2), eventModify(FILE1), eventCreate(FILE1)] | fileChanges([fileCreated(FILE1), fileDeleted(FILE2), fileModified(FILE1)])
    }

    def "watching single file should be possible"() {
        given: "Watch service which returned particular events"
            WatchKey key = Mock(WatchKey) {
                pollEvents() >> [eventCreate(FILE1), eventModify(FILE2), eventModify(FILE3)]
            }
            def parentFile = registrableFile(key)
            def file2 = file(FILE2.name, parentFile)
            WatchService watchService = Mock(WatchService) {
                take() >> key
            }

        when: "Path pinpoint on file2"
            def watchedPaths = [Mock(Path) {
                toString() >> file2.absolutePath
                register(_ as WatchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE) >> key
                toFile() >> file2
            }]
            FileWatcher fileWatcher = new FileWatcher(watchService, watchedPaths, { f -> [file2] }, IgnoredFiles.noIgnoredFiles())

        and: "File watcher is started"
            fileWatcher.run()

        then: "Single file change is returned"
            List<FileChange> fileChangesL = []
            new PollingConditions(timeout: 2).eventually {
                fileChangesL.addAll(fileWatcher.occurredFileChanges())
                assert new FileChanges(fileChangesL) == fileChanges([fileInitialized(file2), fileModified(file2)])
            }
            fileChangesL.size() == 2
    }

    def "equal of file changes should work correctly"() {
        expect:
            (expectedFileChanges == fileChangesToCheck) == result
            (fileChangesToCheck == expectedFileChanges) == result

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
    }

    def file(String name, File parentFile = new File(""), String path = "/$name", boolean exists = true) {
        return Mock(File) {
            isFile() >> exists
            exists() >> exists
            getName() >> name
            getParentFile() >> parentFile
            toPath() >> Mock(Path)
            getAbsolutePath() >> path
        }
    }

    def registrableFile(WatchKey key) {
        return Mock(File) {
            toPath() >> Mock(Path) {
                register(_ as WatchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE) >> key
            }
        }
    }
}
