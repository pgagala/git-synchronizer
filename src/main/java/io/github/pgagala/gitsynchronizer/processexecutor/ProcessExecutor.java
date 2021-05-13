package io.github.pgagala.gitsynchronizer.processexecutor;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Slf4j
public class ProcessExecutor {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    File executionLocation;

    public Response execute(List<String> commands, String description) throws InterruptedException {
        return executeProcess(description, commands, new ProcessBuilder()
            .directory(executionLocation)
            .command(commands));
    }

    public Response execute(List<String> commands, String description, Duration timeout) throws InterruptedException {
        return executeProcess(description, commands, new ProcessBuilder()
                .directory(executionLocation)
                .command(commands),
            timeout);
    }

    private Response executeProcess(String description, List<String> commands, ProcessBuilder processBuilder) throws InterruptedException {
        try {
            return executeAndWaitUntilFinished(description, commands, processBuilder.start());
        } catch (IOException exception) {
            return Response.failure(format("Unsuccessful %s process execution, commands: %s, exception: %s", description, commands, exception));
        }
    }

    private Response executeProcess(String description, List<String> commands, ProcessBuilder processBuilder, Duration timeout) throws InterruptedException {
        try {
            return executeAndWaitForDuration(description, commands, processBuilder.start(), timeout);
        } catch (IOException exception) {
            return Response.failure(format("Unsuccessful %s process execution, commands: %s, exception: %s", description, commands, exception));
        }
    }

    private Response executeAndWaitForDuration(String description, List<String> commands, Process process, Duration duration) throws InterruptedException, IOException {
        StringBuilder responseBuilder = getResponseBuilder(process);
        boolean response = process.waitFor(duration.getSeconds(), TimeUnit.SECONDS);
        if (!response) {
            return printFailureResponseMessage(description, commands, process);
        }
        return Response.success(responseBuilder.toString());
    }

    private Response executeAndWaitUntilFinished(String description, List<String> commands, Process process) throws InterruptedException,
        IOException {
        StringBuilder responseBuilder = getResponseBuilder(process);
        int responseCode = process.waitFor();
        if (responseCode != 0) {
            return printFailureResponseMessage(description, commands, process);
        }
        return Response.success(responseBuilder.toString());
    }

    private Response printFailureResponseMessage(String description, List<String> commands, Process process) throws IOException {
        String errorMsg = String.format("Unsuccessful %s process execution: %s. %nCommand: \"%s\". %nProcess response: %s",
            description, process, String.join(" ", commands), IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8.name()));
        log.error(errorMsg);
        return Response.failure(errorMsg);
    }


    private StringBuilder getResponseBuilder(Process process) throws IOException {
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
            builder.append(LINE_SEPARATOR);
        }
        return builder;
    }
}
