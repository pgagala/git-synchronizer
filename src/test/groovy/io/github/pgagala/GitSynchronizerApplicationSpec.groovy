package io.github.pgagala

import com.beust.jcommander.ParameterException
import spock.lang.Specification


class GitSynchronizerApplicationSpec extends Specification {

    def "Should fail on missing mandatory parameter (#description)"() {
        when: "Git synchronizer is started without any mandatory parameter"
            GitSynchronizerApplication.main(parameters)
        then: "Exception should be thrown"
            thrown ParameterException

        where:
            parameters                                                                       | description
            [] as String[]                                                                   | "all parameters"
            ["--paths", "/bla,/foo"] as String[]                                             | "git server remote"
            ["--gitServerRemote", "git@github.com:pgagala/git-synchronizer.git"] as String[] | "paths"
    }

    def "Should fail on duplicated path parameter"() {
        when: "Git synchronizer is started with duplicated path"
            GitSynchronizerApplication.main(parameters)
        then: "Exception should be thrown"
            thrown ParameterException

        where:
            parameters << [
                    ["--paths", "/bla,/bla", "--gitServerRemote", "git@github.com:pgagala/git-synchronizer.git"] as String[],
//                    ["-p", "/bla,/bla", "--gitServerRemote", "git@github.com:pgagala/git-synchronizer.git"] as String[],
            ]
    }

    def "Should fail on invalid format of path parameter"() {
        when: "Git synchronizer is started without any mandatory parameter"
            GitSynchronizerApplication.main(null)
        then: "Exception should be thrown"
            thrown IllegalArgumentException
    }

    def "Should fail on invalid format of git server remote parameter"() {
        when: "Git synchronizer is started without any mandatory parameter"
            GitSynchronizerApplication.main(null)
        then: "Exception should be thrown"
            thrown IllegalArgumentException
    }

    def "Should fail on null parameter"() {
        when: "Git synchronizer is started without any mandatory parameter"
            GitSynchronizerApplication.main(null)
        then: "Exception should be thrown"
            thrown IllegalArgumentException
    }
}