package io.github.pgagala.gitsynchronizer.processexecutor;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.util.Arrays;

/**
 * Representation of execution output of {@link ProcessExecutor}
 *
 * @author Paweł Gągała
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public class Response {
    boolean successful;
    String result;

    public static Response of(Response... responses) {
        return Arrays.stream(responses)
            .filter(Response::isFailure)
            .findAny()
            .map(r -> Response.failure(r.result))
            .orElse(Response.success());
    }

    public static Response success() {
        return new Response(true, "");
    }

    public static Response success(String result) {
        return new Response(true, result);
    }

    public static Response failure(String failureReason) {
        return new Response(false, failureReason);
    }

    public boolean isSuccessful() {
        return this.successful;
    }

    public boolean isFailure() {
        return !this.isSuccessful();
    }

    public String result() {
        return this.result;
    }
}