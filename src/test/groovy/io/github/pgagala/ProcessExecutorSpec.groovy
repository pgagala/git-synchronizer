package io.github.pgagala

import org.apache.commons.io.FileUtils
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files

import static org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils.randomAlphabetic


class ProcessExecutorSpec extends Specification {

    @Shared
    ProcessExecutor processExecutor

    @Shared
    File testFolder

    def setup() {
        def testDir = "process_executor_test_dir_" + randomAlphabetic(4)
        testFolder = Files.createTempDirectory(testDir).toFile()
        processExecutor = new ProcessExecutor(testFolder)
    }

    def cleanup() {
        if (testFolder.exists()) {
            FileUtils.forceDelete(testFolder)
        }
    }

    def "If process wasn't executed successfully exception should be raised"() {
        when: "Non existing process is executed"
            def response = processExecutor.execute(processParameters, description)

        then: "Reponse should be failure"
            !response.isSuccessful()

        where:
            processParameters | description
            ["bla"]           | "non existing"
            ["ls -b"]         | "wrong argument"
    }

    def "Executed process should return proper response"() {
        given: "File foo in test folder"
            def file = new File("${testFolder.getAbsolutePath()}/foo")
            assert !file.exists()
            file.createNewFile()

        when: "`ls` command is executed"
            def response = processExecutor.execute(["ls"], "ls commands")

        then: "File foo should be listed"
            response.isSuccessful()
            response.result().contains("foo")
    }

}