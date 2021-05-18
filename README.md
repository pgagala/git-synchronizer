![](https://travis-ci.com/pgagala/git-synchronizer.svg?token=jr9dGqtc8QqXdobaunt7&branch=main)

# Why
Lack of applications allow for easy files backup on git repository with open source code. 
(or I haven't searched patiently enough)


# What

Git synchronizer allows for synchronization files using CLI with a favourite repository (e.g. github, gitlab). 
Synchronized can be whole folder or particular file. _Recursively synchronization_<sub>1</sub> isn't supported intentionally - 
user should explicitly pinpoint paths that should be synchronized. Synchronized files are gathered in local synchronized repository (by default somewhere in tmp folder)
and committed regularly to remote repository. Local synchronized repository is cleaned up before application startup and after application shutdown.
Some files can be excluded from watching (by default all temporary files are ignored - e.g. `.sw*`, vi intermediate files).
Tested both on unix and windows.

_Recursively synchronization_<sub>1</sub> - there are folder _/A_ and folder _/A/B_. Synchronized 
folder _/A_ make that _/A/B_ will be synchronized as well.


# How

## Requirements
- java 15
- docker

## Installation

1) Download jar from releases section:
    - [git-synchronizer-1.0.0.jar](https://github.com/pgagala/git-synchronizer/releases/download/1.0.0/git-synchronizer-1.0.0.jar) 
    or
    - build jar by yourself (`./gradlew clean build shadowJar`)

2) Generate ssh key and add it to version control system

    For example: 
    - for github - https://docs.github.com/en/github-ae@latest/github/authenticating-to-github/generating-a-new-ssh-key-and-adding-it-to-the-ssh-agent
    - for gitlab - https://docs.gitlab.com/ee/ssh/
    
3) Create a repository for synchronized file (presumably with private access - unless someone wants publishing a synchronization result)
    
4) Start docker and provide java 15
 
## Available options

```
➜  git-synchronizer git:(main) ✗ java -jar ./build/libs/git-synchronizer-1.0.0.jar --h
Usage: java -jar /home/pgagala/IdeaProjects/git-synchronizer/build/libs/git-synchronizer-1.0.0.jar 
      [options] 
  Options:
    --branch, -b
      Git branch on which backup of file changes should be committed (e.g. 
      --branch myBackupBranch). Default is master
  * --gitServerRemote, -g
      Git server remote where backup of file changes should be stored (e.g. 
      --gitServerRemote git@github.com:pgagala/git-synchronizer.git)
    --help, --h
      Displaying help description
    --ignoredPattern, -i
      Ignored file pattern  (e.g. --ignoredPattern ^bla.*$,^foo.*bar$). Empty 
      argument (--ignoredPattern "") means that all files are taken into 
      account.Default is %s^(\..+\.sw.*|\.~.+|.+~)$
    --network, -n
      Optional docker network. Default is none
  * --paths, -p
      Paths with files which should be monitored (e.g. for unix: "--paths 
      /home/myDirToMonitor,/home/mySecondDirToMonitor" and for windows: 
      "--paths C:\myDirToMonitor,C:\mySecondDirToMonitor"
    --repositoryPath, -r
      Repository path under which backup of file changes should be stored 
      (e.g. --repositoryPath /tmp/mySynchronizedRepo).Default is somewhere in 
      operating system's tmp folder
```

## Running

Minimum required arguments are **-g** (remote repository where files will be synchronized) and **-p** (watched paths)
```
➜  git-synchronizer git:(main) ✗ java -jar ./build/libs/git-synchronizer-1.0.0.jar -g git@gitlab.com:pgagalaGroup/synchronized-files.git -p /tmp/watched,/tmp/watched/folder1
06:40:02.171 [main] INFO  i.g.pgagala.gitsynchronizer.Docker - Building git image...
06:40:02.463 [main] INFO  i.g.pgagala.gitsynchronizer.Docker - Git image built
06:40:02.469 [main] INFO  i.g.p.g.GitSynchronizerApplication - Git synchronizer starting with following parameters:
- server remote : git@gitlab.com:pgagalaGroup/synchronized-files.git
- watching paths: [/tmp/watched, /tmp/watched/folder1]
- repository path: /tmp/git-synchronizer-temp-repository-478430d8-c120-42e0-b0b6-0d30dbedd6d7
- git branch: master
- ignored file patterns: ^(\..+\.sw.*|\.~.+|.+~)$,^(([4-9]9[1-9][3-9])|([5-9]\d\d\d)|(\d{5,}))$


06:40:02.501 [main] INFO  i.g.p.g.RepositoryBootstrap - Initializing repository. Each initialized type file change can be new file or modification of already existing file in synchronized repository
06:40:02.501 [main] INFO  i.g.p.gitsynchronizer.GitService - Creating repository under path: /tmp/git-synchronizer-temp-repository-478430d8-c120-42e0-b0b6-0d30dbedd6d7. Files will be synchronized in that repository. After program shutdown that will be automatically cleaned up
06:40:10.230 [main] INFO  i.g.p.g.GitSynchronizerApplication - Git synchronizer started
```

After any changes in watched paths:
 
```
06:42:39.331 [file-synchronizer-thread-0] INFO  i.g.p.g.FileSynchronizer - New file changes occurred on watched paths:
FileChanges(changes=[File created: /tmp/watched/myNewFile, File changed: /tmp/watched/myNewFile])
```

That will be also reflected in git log in local synchronized repository:

```
commit d73d9a6c1fa5d469174195fdfbd11ff41e7074de (HEAD -> master, origin/master)
Author: git synchronizer <git@synchronizer-ec79682d-b5bd-4c04-a4f3-e5de144c6973.com>
Date:   Tue May 18 04:42:41 2021 +0000

    File created: /tmp/watched/myNewFile
    File changed: /tmp/watched/myNewFile
```

