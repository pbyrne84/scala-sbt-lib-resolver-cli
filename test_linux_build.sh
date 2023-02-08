set -e
sbt testAndBuildAssembly
native-image  --no-server \
  --no-fallback \
  --allow-incomplete-classpath \
  --report-unsupported-elements-at-runtime \
  --static \
  --initialize-at-build-time=scala.runtime.Statics \
  -H:ConfigurationFileDirectories=src/main/resources/META-INF/native-image \
  -H:+ReportExceptionStackTraces \
  -H:TraceClassInitialization=true \
  -jar target/scala-2.13/scala-sbt-lib-resolver-cli.jar
./scala-sbt-lib-resolver-cli --hotlist test-single --config internal --debug
