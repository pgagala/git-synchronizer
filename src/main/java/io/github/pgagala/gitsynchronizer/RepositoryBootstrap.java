package io.github.pgagala.gitsynchronizer;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Initializing local repository synchronized with remote repository.
 * Take care about cleaning repository before initialization.
 *
 * @author Paweł Gągała
 */
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Slf4j
class RepositoryBootstrap {

    GitService gitService;

    void initialize() throws InterruptedException, IOException {
        log.info("Initializing repository. Each initialized type file change can be new file or modification of already existing file in synchronized repository");
        cleanup();
        gitService.createRepository();
        gitService.pull();
    }

    void cleanup() {
        gitService.deleteRepository();
    }
}