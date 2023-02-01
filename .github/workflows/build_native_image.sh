set -e
native-image  --no-server \
  --no-fallback \
  --allow-incomplete-classpath \
  --report-unsupported-elements-at-runtime \
  --static \
  --initialize-at-build-time=scala.runtime.Statics \
  -H:ConfigurationFileDirectories=src/main/resources/META-INF/native-image \
  -H:+ReportExceptionStackTraces \
  -H:TraceClassInitialization=true \
  -jar binaries/scala-sbt-lib-resolver-cli.jar
ls -l
./scala-sbt-lib-resolver-cli --hotlist test-single --config internal --debug
cd ..
ls -l
scala-sbt-lib-resolver-cli/scala-sbt-lib-resolver-cli --hotlist test-single --config src/main/resources/config.json --debug
cd scala-sbt-lib-resolver-cli
cp scala-sbt-lib-resolver-cli binaries/scala-sbt-lib-resolver-cli-linux
git add -f binaries/scala-sbt-lib-resolver-cli-linux
