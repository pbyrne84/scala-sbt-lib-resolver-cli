## Copied from https://github.com/pbyrne84/scala_native_lambda_test/blob/bd3297d8d0ccb90d02cfc48d94250e9e3f110bd0/scala_native_lambda_test_builder/Dockerfile#L10


FROM amazonlinux:2
RUN yum update -y
RUN yum install -y unzip zip tar sudo
RUN curl -s "https://get.sdkman.io" | bash
RUN pwd
RUN ls -l
RUN cat ~/.sdkman/etc/config
RUN source ~/.sdkman/bin/sdkman-init.sh && sdk list java
RUN source ~/.sdkman/bin/sdkman-init.sh && sdk install java 22.3.r11-grl
RUN source ~/.sdkman/bin/sdkman-init.sh && sdk install sbt
RUN source ~/.sdkman/bin/sdkman-init.sh && java --version
RUN source ~/.sdkman/bin/sdkman-init.sh && gu install native-image
RUN yum install -y -q yum-utils
RUN yum-config-manager --enable epel > /dev/null
RUN yum install -y zlib1g-dev gcc glibc-devel
RUN yum -y group install "development tools"
RUN yum install -y https://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm
RUN yum install -y gcc-c++ binutils-devel glibc-devel glibc-static zlib-static

RUN mkdir ~/project_mount
RUN yum  install -y locales
RUN localectl set-locale LANG=en_US.utf8

#export LC_ALL=en_US.UTF-8
