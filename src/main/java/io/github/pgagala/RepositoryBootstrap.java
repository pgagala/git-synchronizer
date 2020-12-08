package io.github.pgagala;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
class RepositoryBootstrap {

    String path;

    void initialize() {
        deleteRepository();
    }

    private void deleteRepository() {

    }

    void cleanup() {
        deleteRepository();
    }
}