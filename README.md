![](https://travis-ci.com/pgagala/git-synchronizer.svg?token=jr9dGqtc8QqXdobaunt7&branch=main)

# Why
Lack of applications allow for easy files backup on git repository with open source code. 
(or I haven't searched patiently enough)


# What

## Description
Git synchronizer allows for synchronization files using CLI with a favourite repository (e.g. github, gitlab). 
Synchronized can be whole folder or particular file. Recursively synchronization* isn't supported intentionally - 
user should explicitly pinpoint paths that should be synchronized. Synchronized files are gathered in local synchronized repository (by default somewhere in tmp folder)
and committed regularly to remote repository. Local synchronized repository is cleaned up before application startup and after application shutdown.
Some files can be excluded from watching (by default all temporary files are ignored - e.g. `.sw*`, vi intermediate files).
Tested both on unix and windows.

* Recursively synchronization - there are folder /A and folder /A/B. Synchronized 
folder /A make that /A/B will be synchronized as well. 

# How

## Requirements
- java 15
- docker

## Installation

### Get jar



## Running

