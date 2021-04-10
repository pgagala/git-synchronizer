package io.github.pgagala.gitsynchronizer

import com.beust.jcommander.ParameterException
import io.github.pgagala.gitsynchronizer.GitSynchronizerApplication
import spock.lang.Specification

import java.nio.file.Path


//TODO add cases for gitrepository path and branch
@SuppressWarnings("GroovyAccessibility")
class GitSynchronizerApplicationArgsParserSpec extends Specification {


    public static final String GIT_SERVER_REMOTE = "git@github.com:pgagala/git-synchronizer.git"
    public static final String GIT_SERVER_REMOTE2 = "http://172.25.0.2/test_repository.git"
    public static final String PATH_LITERAL = "/bla"
    public static final String PATH_LITERAL2 = "/bla2"
    public static final Path PATH = Path.of(PATH_LITERAL)
    public static final Path PATH2 = Path.of(PATH_LITERAL2)

    def "Should return correctly parsed mandatory parameters"() {
        given: "argument parser"
            GitSynchronizerApplication.GitSynchronizerApplicationArgsParser parser = new GitSynchronizerApplication.GitSynchronizerApplicationArgsParser(parameters)

        expect: "parsed parameters are as expected"
            parser.paths() == paths
            parser.serverRemote().getValue() == serverRemote

        where:
            parameters                                                                    | paths         | serverRemote
            ["--paths", PATH_LITERAL, "--gitServerRemote", GIT_SERVER_REMOTE] as String[] | [PATH]        | GIT_SERVER_REMOTE
            ["-p", PATH_LITERAL, "--gitServerRemote", GIT_SERVER_REMOTE] as String[]      | [PATH]        | GIT_SERVER_REMOTE
            ["-p", PATH_LITERAL, "-g", GIT_SERVER_REMOTE] as String[]                     | [PATH]        | GIT_SERVER_REMOTE
            ["-p", PATH_LITERAL, "-g", GIT_SERVER_REMOTE2] as String[]                    | [PATH]        | GIT_SERVER_REMOTE2
            ["-p", "$PATH_LITERAL,$PATH_LITERAL2", "-g", GIT_SERVER_REMOTE] as String[]   | [PATH, PATH2] | GIT_SERVER_REMOTE
    }

    def "Should flat duplicated path parameter"() {
        when: "parameters are passed to parser"
            def parameters = ["-p", "/bla,/bla", "--gitServerRemote", GIT_SERVER_REMOTE] as String[]
            def parser = new GitSynchronizerApplication.GitSynchronizerApplicationArgsParser(parameters)
        then:
            noExceptionThrown()
        and: "paths are flattened"
            parser.paths().size() == 1
    }

    def "Should fail on missing mandatory parameter (#description)"() {
        when: "parameters are passed to parser"
            new GitSynchronizerApplication.GitSynchronizerApplicationArgsParser(parameters)
        then: "exception should be thrown"
            thrown ParameterException

        where:
            parameters                                           | description
            [] as String[]                                       | "all parameters"
            ["--paths", "/bla,/foo"] as String[]                 | "git server remote"
            ["--gitServerRemote", GIT_SERVER_REMOTE] as String[] | "paths"
    }

    def "Should fail on doubled git server remote parameter"() {
        when: "parameters are passed to parser"
            new GitSynchronizerApplication.GitSynchronizerApplicationArgsParser(parameters)
        then: "exception should be thrown"
            thrown ParameterException

        where:
            parameters << [
                    ["-p", "/bla,/foo", "-g", "git@github.com:pgagala/git-synchronizer.git,git@github.com:pgagala/git-synchronizer2.git"] as String[],
                    ["-p", "/bla,/foo", "-g", "git@github.com:pgagala/git-synchronizer", "git,git@github.com:pgagala/git-synchronizer2.git"] as String[],
                    ["-p", "/bla,/foo", "-g", "git@github.com:pgagala/git-synchronizer.git", "-g", "git@github.com:pgagala/git-synchronizer2.git"] as String[]
            ]
    }

    def "Should fail on invalid format of parameter"() {
        when: "parameters with invalid path format are passed to parser"
            new GitSynchronizerApplication.GitSynchronizerApplicationArgsParser(parameters)
        then: "exception should be thrown"
            thrown ParameterException

        where:
            parameters << [
                    ["-p", "bla/bla2", "-g", GIT_SERVER_REMOTE] as String[],
                    ["-p", "bla,/bla2", "-g", GIT_SERVER_REMOTE] as String[],
                    ["-p", "", "-g", GIT_SERVER_REMOTE] as String[],
                    ["-p", "-", "-g", GIT_SERVER_REMOTE] as String[],
                    ["-p", PATH_LITERAL, "-g", "git@github.com:pgagala/git-synchronizer"] as String[],
                    ["-p", PATH_LITERAL, "-g", "gitgithub.com:pgagalagit-synchronizer.git"] as String[],
                    ["-p", PATH_LITERAL, "-g", "git@githubcom:pgagala/git-synchronizer.gi"] as String[],
            ]
    }

    def "Should fail on null parameters"() {
        when: "null parameters are passed to parser"
            new GitSynchronizerApplication.GitSynchronizerApplicationArgsParser(null)
        then: "exception should be thrown"
            thrown IllegalArgumentException
    }
}