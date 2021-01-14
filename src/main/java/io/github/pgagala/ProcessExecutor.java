package io.github.pgagala;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import static java.lang.String.format;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Slf4j
class ProcessExecutor {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    File executionLocation;

    Response execute(List<String> commands, String description) throws InterruptedException {
        return executeProcess(description, new ProcessBuilder()
            .directory(executionLocation)
            .command(commands));
    }

    private Response executeProcess(String description, ProcessBuilder processBuilder) throws InterruptedException {
        try {
            return executeAndWaitUntilFinished(description, processBuilder.start());
        } catch (IOException exception) {
            return Response.failure(format("Unsuccessful %s process execution, exception: %s", description, exception));
        }
    }

    private Response executeAndWaitUntilFinished(String description, Process process) throws InterruptedException, IOException {
        StringBuilder responseBuilder = getResponseBuilder(process);
        int responseCode = process.waitFor();
        if (responseCode != 0) {
            log.error("Unsuccessful {} process execution: {}", description, process);
            return Response.failure(format("Unsuccessful %s process execution: %s", description, process));
        }
        return Response.success(responseBuilder.toString());
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
