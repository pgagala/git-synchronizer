package io.github.pgagala.gitsynchronizer;

import lombok.Value;

@Value
class GitBranch {

    public static final GitBranch DEFAULT_BRANCH = new GitBranch("master");

    String value;
}