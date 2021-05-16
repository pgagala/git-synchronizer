package io.github.pgagala.gitsynchronizer

import spock.lang.Specification

import java.util.regex.Pattern


class IgnoredFilesSpec extends Specification implements FileChangesSampleData {

    def "events correspond to intermediate files should be removed"() {
        given: "ignored files with intermediate ignored files"
            def ignoredFiles = IgnoredFiles.intermediateIgnoredFiles()

        expect: "removed events referring to intermediate ignored files"
            def removedEvents = ignoredFiles.removeEventsRefersToIgnoredFiles(incomingEvents)
            removedEvents.size() == expectedEventsListAfterRemoval.size()
            expectedEventsListAfterRemoval.eachWithIndex { it, index ->
                assert it.context() == removedEvents[index].context()
            } != null

        where:
            incomingEvents                                                                                                                | expectedEventsListAfterRemoval
            []                                                                                                                            | []
            [eventCreate(".file2.swp"), eventModify(".file2.swpx"), eventCreate(".~file2"), eventCreate("~file3"), eventModify("file3~")] | [eventCreate("~file3")]
            [eventCreate(".file2.sw"), eventModify(".file2.sw2"), eventModify(".~file2"), eventModify("file3"), eventDelete(".~file2")]   | [eventModify("file3")]
            [eventCreate("file1"), eventCreate(".file2.swp"), eventModify(".file2.swpx"), eventDelete(".file3.swpx")]                     | [eventCreate("file1")]
            [eventCreate("file1"), eventCreate("4913"), eventDelete("4913"), eventCreate("5222"), eventDelete("5232")]                    | [eventCreate("file1")]
    }

    def "no events should be ignored"() {
        given: "ignored files"
            def ignoredFiles = IgnoredFiles.noIgnoredFiles()

        expect: "no files are ignored"
            def removedEvents = ignoredFiles.removeEventsRefersToIgnoredFiles(incomingEvents)
            removedEvents.size() == expectedEventsListAfterRemoval.size()
            expectedEventsListAfterRemoval.eachWithIndex { it, index ->
                assert it.context() == removedEvents[index].context()
            } != null

        where:
            incomingEvents                                                                                            | expectedEventsListAfterRemoval
            []                                                                                                        | []
            [eventCreate("file1"), eventCreate(".file2.swp"), eventModify(".file2.swpx"), eventDelete(".file3.swpx")] | [eventCreate("file1"), eventCreate(".file2.swp"), eventModify(".file2.swpx"), eventDelete(".file3.swpx")]
            [eventCreate(".file2.swp"), eventModify(".file2.swpx")]                                                   | [eventCreate(".file2.swp"), eventModify(".file2.swpx")]
            [eventCreate(".file2.sw"), eventModify(".~file3"), eventModify(".file2.sw2")]                             | [eventCreate(".file2.sw"), eventModify(".~file3"), eventModify(".file2.sw2")]
    }

    def "events correspond to predefined patterns should be ignored"() {
        given: "ignored files with swap ignored files"
            def ignoredFiles = new IgnoredFiles(patterns)

        expect: "removed events referring to swap ignored files"
            def removedEvents = ignoredFiles.removeEventsRefersToIgnoredFiles(incomingEvents)
            removedEvents.size() == expectedEventsListAfterRemoval.size()
            expectedEventsListAfterRemoval.eachWithIndex { it, index ->
                assert it.context() == removedEvents[index].context()
            } != null

        where:
            patterns                    | incomingEvents                                                                       | expectedEventsListAfterRemoval
            []                          | []                                                                                   | []
            [Pattern.compile("file1")]  | [eventCreate("file1")]                                                               | []
            [Pattern.compile("^bla.*")] | [eventCreate("blaA"), eventModify("blaB"), eventDelete("blaC"), eventModify("fooA")] | [eventModify("fooA")]
    }

    def "file matches ignored pattern should treated as ignored"() {
        expect:
            new IgnoredFiles(patterns).shouldBeIgnored(file) == isIgnored

        where:
            patterns                   | file              | isIgnored
            [Pattern.compile("file1")] | new File("file1") | true
            [Pattern.compile(".*")]    | new File("abc")   | true
            []                         | new File("abc")   | false
    }

}