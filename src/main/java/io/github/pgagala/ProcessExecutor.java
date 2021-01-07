package io.github.pgagala;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Slf4j
class ProcessExecutor {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    List<String> commands;
    String description;

    Response execute() throws IOException, InterruptedException {
        return executeProcess(description, new ProcessBuilder().command(commands));
    }

    private Response executeProcess(String description, ProcessBuilder processBuilder) throws IOException, InterruptedException {
        return executeAndWaitUntilFinished(description, processBuilder.start());
    }

    private Response executeAndWaitUntilFinished(String description, Process process) throws InterruptedException, IOException {
        StringBuilder responseBuilder = getResponseBuilder(process);
        int responseCode = process.waitFor();
        if (responseCode != 0) {
            log.error("Unsuccessful {} process execution: {}", description, process);
            return Response.failure();
        }
        return Response.of(responseBuilder.toString());
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

    static ProcessExecutorBuilder builder(File workingDir, String description) {
        return new ProcessExecutorBuilder(workingDir, description);
    }

    @RequiredArgsConstructor
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    static class ProcessExecutorBuilder {

        File workingDirectory;
        String description;
        List<String> commands = new ArrayList<>();

        ProcessExecutor build() {
            return new ProcessExecutor(commands, description);
        }

    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    static class Response {
        boolean successful;
        String result;

        static Response of(String result) {
            return new Response(true, result);
        }

        static Response failure() {
            return new Response(false, "");
        }

        boolean isSuccessful() {
            return this.successful;
        }

        String result() {
            return this.result;
        }
    }


}
