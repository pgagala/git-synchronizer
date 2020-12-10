package io.github.pgagala;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;

/**
 * Creating/destroying temporary git repository (point of file synchronization with remote). Using docker to run git commands.
 */
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Slf4j
class GitService {

    String gitRepositoryPath;

    void createRepository() throws IOException, InterruptedException {
        log.info("Creating repository under path: {}. Files will be synchronized in that repository. " +
            "After program shutdown that will be automatically cleaned up", gitRepositoryPath);
        try {
            Process initRepository = new ProcessBuilder().command("docker", "run", "--rm", "-v", gitRepositoryPath + ":/git",
                "alpine/git", "init").start();
            executeAndWaitUntilFinished(initRepository);
        } catch (Exception exc) {
            log.error("Exception during creating repository: {}", exc.getMessage());
            throw exc;
        }
    }

    void deleteRepository() throws IOException {
        File repositoryFolder = new File(gitRepositoryPath);
//        try {
//            FileUtils.forceDelete(repositoryFolder);
            deleteFolder(repositoryFolder);
//        } catch (IOException exc) {
//            log.error("Unsuccessful deleting repository under path: {}", gitRepositoryPath);
//            throw exc;
//        }
    }

     static void deleteFolder(File file){
      for (File subFile : file.listFiles()) {
         if(subFile.isDirectory()) {
            deleteFolder(subFile);
         } else {
            subFile.delete();
         }
      }
      file.delete();
   }

    private void executeAndWaitUntilFinished(Process process) throws InterruptedException {
        int responseCode = process.waitFor();
        if (responseCode != 0) {
            log.error("Unsuccessful process execution: {}", process);
        }
    }
}