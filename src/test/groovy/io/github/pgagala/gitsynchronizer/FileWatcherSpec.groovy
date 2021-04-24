package io.github.pgagala.gitsynchronizer


import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.nio.file.Path
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.util.function.Function

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY

class FileWatcherSpec extends Specification implements FileChangesSampleData {

    @Shared
    final File FILE1 = file("file1")

    @Shared
    final File FILE2 = file("file2")

    @Shared
    final File FILE2_SWP = file(".file2.swp")

    @Shared
    final File FILE2_SWPX = file(".file2.swpx")

    def "On start files from watched paths should be added (without ignored files) as created events"() {
        given: "file watcher with paths"
            FileWatcher fileWatcher = new FileWatcher(Mock(WatchService), [
                    Mock(Path) {
                        toFile() >> Mock(File) {
                            isFile() >> false
                        }
                        toString() >> "/"
                    }
            ], { f -> [FILE1, FILE2, FILE2_SWP] }, IgnoredFiles.swapIgnoredFiles())

        when: "file watcher is started"
            fileWatcher.run()
        then: "files situated under paths should be returned as created file events"
            FileChanges occurredFileChanges = fileWatcher.occurredFileChanges()
            occurredFileChanges == fileChanges([FileCreated.of(FILE1), FileCreated.of(FILE2)])
            occurredFileChanges.newOrModifiedFiles() == [FILE1, FILE2]
    }

    def "Exception should be thrown on file watcher start if there are any files in watched paths with same file name"() {
        when: "file watcher with duplicated files is created"
            new FileWatcher(Mock(WatchService), [Mock(Path) {
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
            FileWatcher fileWatcher = new FileWatcher(watchService, watchedPaths, { f -> [] }, IgnoredFiles.swapIgnoredFiles())

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
                pollEvents() >> [eventCreate(FILE1), eventCreate(FILE2), eventCreate(new File("file3"))]
            }
            def parentFile = registrableFile(key)
            def file1 = file("file1", parentFile)
            def file2 = file("file2", parentFile)
            WatchService watchService = Mock(WatchService) {
                take() >> key
            }

        when: "Path pinpoint on file 1"
            def watchedPaths = [Mock(Path) {
                toString() >> file1.absolutePath
                register(_ as WatchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE) >> key
                toFile() >> file1
            }]
            FileWatcher fileWatcher = new FileWatcher(watchService, watchedPaths, { f -> [file1] }, IgnoredFiles.noIgnoredFiles())

        and: "File watcher is started"
            fileWatcher.run()

        then: "Single file change is returned"
            new PollingConditions(timeout: 2).eventually {
                assert fileWatcher.occurredFileChanges() == fileChanges([fileCreated(file1)])
            }

        when: "Path pinpoint on file 1, file2"
            watchedPaths = [Mock(Path) {
                toString() >> file1.absolutePath
                register(_ as WatchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE) >> key
                toFile() >> file1
            },
                            Mock(Path) {
                                toString() >> file2.absolutePath
                                register(_ as WatchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE) >> key
                                toFile() >> file2
                            }]
            def filesFetcher = Mock(Function<File, Collection<File>>) {
                apply(_ as File) >>> [[file1], [file2], []]
            }
            fileWatcher = new FileWatcher(watchService, watchedPaths, filesFetcher, IgnoredFiles.noIgnoredFiles())

        and: "File watcher is started"
            fileWatcher.run()

        then: "Single file change is returned"
            new PollingConditions(timeout: 2).eventually {
                assert fileWatcher.occurredFileChanges() == fileChanges([fileCreated(file1), fileCreated(file2)])
            }
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
