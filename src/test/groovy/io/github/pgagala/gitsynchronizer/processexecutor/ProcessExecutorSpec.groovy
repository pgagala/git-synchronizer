package io.github.pgagala.gitsynchronizer.processexecutor


import org.apache.commons.io.FileUtils
import spock.lang.Requires
import spock.lang.Specification
import spock.util.environment.OperatingSystem

import java.nio.file.Files

import static org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils.randomAlphabetic

class ProcessExecutorSpec extends Specification {

    ProcessExecutor processExecutor
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

    @Requires({ OperatingSystem.getCurrent().isWindows() })
    def "Executed process should return proper response (windows)"() {
        given: "File foo in test folder"
            def file = new File("${testFolder.getAbsolutePath()}/foo")
            assert !file.exists()
            file.createNewFile()

        when: "`ls` command is executed"
            def response = processExecutor.execute(["cmd.exe", "/c", "dir"], "dir commands")

        then: "File foo should be listed"
            response.isSuccessful()
            response.result().contains("foo")
    }

    @Requires({ !OperatingSystem.getCurrent().isWindows() })
    def "Executed process should return proper response (unix)"() {
        given: "File foo in test folder"
            def file = new File("${testFolder.getAbsolutePath()}/foo")
            assert !file.exists()
            file.createNewFile()

        when: "`ls` command is executed"
            def response = processExecutor.execute(["dir"], "ls commands")

        then: "File foo should be listed"
            response.isSuccessful()
            response.result().contains("foo")
    }

}