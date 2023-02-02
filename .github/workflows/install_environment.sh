set -e
sudo apt-get update
sudo apt-get install build-essential libz-dev zlib1g-dev
git lfs install
date
date +%s%N | cut -b1-13
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk version
sdk install scalacli
sdk list java
sdk install java 22.3.r11-grl
gu install native-image

echo "java version"
java --version

echo "gu version"
gu --version

echo "native-image version"
native-image --version
