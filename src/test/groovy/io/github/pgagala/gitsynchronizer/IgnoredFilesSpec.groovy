package io.github.pgagala.gitsynchronizer

import spock.lang.Specification

import java.util.regex.Pattern


class IgnoredFilesSpec extends Specification implements FileChangesSampleData {

    def "events correspond to swap files should be removed"() {
        given: "ignored files with swap ignored files"
            def ignoredFiles = IgnoredFiles.swapIgnoredFiles()

        expect: "removed events referring to swap ignored files"
            def removedEvents = ignoredFiles.removeEventsRefersToIgnoredFiles(incomingEvents)
            removedEvents.size() == expectedEventsListAfterRemoval.size()
            expectedEventsListAfterRemoval.eachWithIndex { it, index ->
                assert it.context() == removedEvents[index].context()
            } != null

        where:
            incomingEvents                                                                                            | expectedEventsListAfterRemoval
            []                                                                                                        | []
            [eventCreate("file1"), eventCreate(".file2.swp"), eventModify(".file2.swpx"), eventDelete(".file3.swpx")] | [eventCreate("file1")]
            [eventCreate(".file2.swp"), eventModify(".file2.swpx")]                                                   | []
            [eventCreate(".file2.sw"), eventModify(".file2.sw2")]                                                     | [eventCreate(".file2.sw"), eventModify(".file2.sw2")]
    }

    def "no events should be ignored"() {
        given: "ignored files with swap ignored files"
            def ignoredFiles = IgnoredFiles.noIgnoredFiles()

        expect: "removed events referring to swap ignored files"
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
            [eventCreate(".file2.sw"), eventModify(".file2.sw2")]                                                     | [eventCreate(".file2.sw"), eventModify(".file2.sw2")]
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