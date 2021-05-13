package io.github.pgagala.gitsynchronizer;

import java.util.Map;

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