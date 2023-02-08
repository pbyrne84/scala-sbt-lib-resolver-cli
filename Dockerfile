FROM ubuntu:22.04
SHELL ["/bin/bash", "-c"]
RUN apt-get update -y
RUN apt-get install -y unzip zip tar sudo curl
RUN apt-get install -y build-essential libz-dev zlib1g-dev
RUN curl -s "https://get.sdkman.io" | bash
RUN pwd
RUN ls -l
RUN cat ~/.sdkman/etc/config
RUN . ~/.sdkman/bin/sdkman-init.sh
RUN source ~/.sdkman/bin/sdkman-init.sh && sdk list java
RUN source ~/.sdkman/bin/sdkman-init.sh && sdk install java 22.3.r11-grl
RUN source ~/.sdkman/bin/sdkman-init.sh && sdk install sbt
RUN source ~/.sdkman/bin/sdkman-init.sh && java --version
RUN source ~/.sdkman/bin/sdkman-init.sh && gu install native-image
RUN mkdir ~/project_mount
# Cats generates files that are are not simple ascii names and breaks assembly unless we change from POSIX
RUN apt-get install -y locales
RUN locale-gen en_US.UTF-8
ENV LANG en_US.UTF-8
ENV LC_ALL en_US.UTF-8
