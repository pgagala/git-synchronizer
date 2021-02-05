package io.github.pgagala;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.io.IOException;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
class RepositoryBootstrap {

    GitService gitService;

    void initialize() throws InterruptedException, IOException {
        deleteRepository();
        gitService.createRepository();
    }

    void cleanup() {
        deleteRepository();
    }

    private void deleteRepository() {
        gitService.deleteRepository();
    }
}