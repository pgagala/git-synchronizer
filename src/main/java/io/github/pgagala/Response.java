package io.github.pgagala;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.Arrays;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class Response {
    boolean successful;
    String result;

    static Response of(Response... responses) {
        return Arrays.stream(responses)
            .filter(Response::isFailure)
            .findAny()
            .map(r -> Response.failure(r.result))
            .orElse(Response.success());
    }

    static Response success() {
        return new Response(true, "");
    }

    static Response success(String result) {
        return new Response(true, result);
    }

    static Response failure(String failureReason) {
        return new Response(false, failureReason);
    }

    boolean isSuccessful() {
        return this.successful;
    }

    boolean isFailure() {
        return !this.isSuccessful();
    }

    String result() {
        return this.result;
    }
}