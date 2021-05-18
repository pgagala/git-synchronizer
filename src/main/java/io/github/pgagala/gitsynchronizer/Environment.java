package io.github.pgagala.gitsynchronizer;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * Provides convenient access for environment variables
 *
 * @author Paweł Gągała
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class Environment {
    static String getUserHome() {
        String osName = System.getenv().entrySet()
            .stream().filter(e -> e.getKey().startsWith("OS"))
            .findAny()
            .map(Map.Entry::getValue)
            .orElse("");
        return osName.startsWith("Windows") ?
            System.getenv("USERPROFILE") :
            System.getenv("HOME");
    }
}