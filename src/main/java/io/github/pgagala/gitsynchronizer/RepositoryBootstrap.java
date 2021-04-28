package io.github.pgagala.gitsynchronizer;

import io.github.pgagala.gitsynchronizer.processexecutor.Response;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Slf4j
class RepositoryBootstrap {

    GitService gitService;

    //TODO add spec if on initialization cannot remove repo then excp should be raised
    void initialize() throws InterruptedException, IOException {
        log.info("Initializing repository. Each initialized type file change can be new file or modification of already existing file in synchronized repository");
        deleteRepository();
        gitService.createRepository();
        gitService.pull();
    }

    void cleanup() {
        deleteRepository();
    }

    //TODO exception if deletion wasn't successful
    private void deleteRepository() {
        Response response = gitService.deleteRepository();
    }
}