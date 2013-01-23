Welcome to the AgentJ, Version 1.0

See doc/userman/AgentJManual.pdf for a quick-start into the AgentJ software and guidelines for the software.

Building using Ant:

cd to the agentj directory

to just build core agentj, type:

ant build

to build all the sources and modules, type:

ant

to clean the compiled sources, type:

ant cleanAll

to run a complete clean and build type:

ant cleanBuild

If you are running Java 1.6 (Mustang) and you want to use the dns hack, edit the main build file by
uncommenting the dns-mustang build in the build-utils target, and comment the dns build.
