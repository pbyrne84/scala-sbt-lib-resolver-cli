#!/usr/bin/env bash
apt-get update -y
apt-get install -y unzip zip tar sudo curl mlocate
apt-get install -y gcc zlib1g-dev build-essential
bash
curl -s "https://get.sdkman.io" | bash
pwd
ls -l
chmod +755 ~/.sdkman/bin/sdkman-init.sh
source "/root/.sdkman/bin/sdkman-init.sh"

sdk list java
sdk install java 22.3.r11-grl
sdk install sbt
java --version
gu install native-image
apt-get install -y locales
locale-gen en_US.UTF-8
locale-gen en_US en_US.UTF-8
update-locale LANG=C.UTF-8
