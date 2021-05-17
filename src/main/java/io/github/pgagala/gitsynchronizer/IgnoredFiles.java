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

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@SuppressWarnings("java:S1452")
class IgnoredFiles {

    List<Pattern> ignoredFilePatterns;

    static final String INTERMEDIATE_FILES_PATTERN = "^(\\..+\\.sw.*|\\.~.+|.+~)$";
    //4913+ is intermediate VIM program file - https://github.com/neovim/neovim/blob/536c0ba27e79929eb30850d8e11f2ed026930ab3/src/nvim/fileio.c#L2710
    static final String VIM_INTERMEDIATE_FILES_PATTERN = "^(([4-9]9[1-9][3-9])|([5-9]\\d\\d\\d)|(\\d{5,}))$";

    static IgnoredFiles from(List<Pattern> patterns) {
        if(patterns.isEmpty()) {
            return noIgnoredFiles();
        }
        return new IgnoredFiles(new ArrayList<>(patterns));
    }

    static IgnoredFiles intermediateIgnoredFiles() {
        return new IgnoredFiles(List.of(
            Pattern.compile(INTERMEDIATE_FILES_PATTERN),
            Pattern.compile(VIM_INTERMEDIATE_FILES_PATTERN)
        ));
    }

    private static IgnoredFiles noIgnoredFiles() {
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

    @Override
    public String toString() {
        return String.join(",", ignoredFilePatterns.stream().map(Pattern::toString).collect(Collectors.toUnmodifiableList()));
    }
}