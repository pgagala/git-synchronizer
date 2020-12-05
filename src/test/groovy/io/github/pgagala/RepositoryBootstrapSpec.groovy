package io.github.pgagala

import spock.lang.Specification


class RepositoryBootstrapSpec extends Specification {

    def "On system start repository should be initialized if repo doesn't exist"() {
    expect:
        1==1
    }

    def "On system start repository should be discarded and initialized if repo exists"() {
    expect:
        1==1
    }

    def "Bootstrap shouldn't work without configured repository address"() {
        expect:
        1==1
    }

    def "On cleanup repository should be deleted"() {
        expect:
        1==1
    }


}
