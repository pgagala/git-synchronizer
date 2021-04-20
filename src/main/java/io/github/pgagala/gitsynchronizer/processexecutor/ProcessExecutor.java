package io.github.pgagala.gitsynchronizer.processexecutor;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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
            return executeAndWaitUntilFinished(description, processBuilder.start());
        } catch (IOException exception) {
            return Response.failure(format("Unsuccessful %s process execution, commands: %s, exception: %s", description, commands, exception));
        }
    }

    private Response executeProcess(String description, List<String> commands, ProcessBuilder processBuilder, Duration timeout) throws InterruptedException {
        try {
            return executeAndWaitForDuration(description, processBuilder.start(), timeout);
        } catch (IOException exception) {
            return Response.failure(format("Unsuccessful %s process execution, commands: %s, exception: %s", description, commands, exception));
        }
    }

    private Response executeAndWaitForDuration(String description, Process process, Duration duration) throws InterruptedException, IOException {
        StringBuilder responseBuilder = getResponseBuilder(process);
        boolean response = process.waitFor(duration.getSeconds(), TimeUnit.SECONDS);
        if (!response) {
            return printFailureResponseMessage(description, process);
        }
        return Response.success(responseBuilder.toString());
    }

    private Response executeAndWaitUntilFinished(String description, Process process) throws InterruptedException, IOException {
        StringBuilder responseBuilder = getResponseBuilder(process);
        int responseCode = process.waitFor();
        if (responseCode != 0) {
            return printFailureResponseMessage(description, process);
        }
        return Response.success(responseBuilder.toString());
    }

    @NotNull
    private Response printFailureResponseMessage(String description, Process process) {
        log.error("Unsuccessful {} process execution: {}", description, process);
        return Response.failure(format("Unsuccessful %s process execution: %s", description, process));
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
