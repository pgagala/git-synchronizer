package io.github.pgagala

class FileExtension {
    static def shallowEquals(File self, File file2) {
        return self.name == file2.name &&
                self.size() == file2.size()
    }
}