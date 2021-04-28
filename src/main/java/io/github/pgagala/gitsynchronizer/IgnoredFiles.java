package io.github.pgagala.gitsynchronizer;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.io.File;
import java.nio.file.WatchEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@SuppressWarnings("java:S1452")
class IgnoredFiles {

    List<Pattern> ignoredFilePatterns;

    static final String INTERMEDIATE_FILES_PATTERN = "^\\..+\\.sw.*|\\.~.+$";

    static IgnoredFiles intermediateIgnoredFiles() {
        return new IgnoredFiles(List.of(Pattern.compile(INTERMEDIATE_FILES_PATTERN)));
    }

    static IgnoredFiles noIgnoredFiles() {
        return new IgnoredFiles(List.of());
    }

    boolean shouldBeIgnored(File file) {
        return ignoredFilePatterns.stream()
            .anyMatch(p -> p.matcher(file.getName()).matches());
    }

    List<WatchEvent<?>> removeEventsRefersToIgnoredFiles(List<WatchEvent<?>> watchEvents) {
        List<WatchEvent<?>> copiedWatchEvents = new ArrayList<>(watchEvents);
        return copiedWatchEvents.stream()
            .filter(e -> ignoredFilePatterns.stream().noneMatch(p -> p.matcher(e.context().toString()).matches()))
            .collect(Collectors.toUnmodifiableList());
    }
}