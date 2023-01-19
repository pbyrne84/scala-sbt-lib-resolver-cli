name := "scala-project-generator"

scalaVersion := "2.13.10"

val zioVersion = "2.0.5"
val zioLoggingVersion = "2.1.7"
val circeVersion = "0.14.3"

addCompilerPlugin("io.tryp" % "splain" % "1.0.1" cross CrossVersion.patch)

libraryDependencies ++= List(
  "com.monovore" %% "decline" % "2.4.1",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-generic-extras" % circeVersion,
  "ch.qos.logback" % "logback-classic" % "1.4.5",
  "net.logstash.logback" % "logstash-logback-encoder" % "7.2",
  "dev.zio" %% "zio-logging-slf4j" % zioLoggingVersion,
  "dev.zio" %% "zio-config-typesafe" % "3.0.6",
  "com.softwaremill.sttp.client3" %% "zio" % "3.8.7",
  "dev.zio" %% "zio" % zioVersion,
  "com.github.tomakehurst" % "wiremock" % "2.27.2" % Test,
  "org.mockito" % "mockito-core" % "5.0.0" % Test,
  "dev.zio" %% "zio-test" % zioVersion % Test,
  "dev.zio" %% "zio-test-sbt" % zioVersion % Test
)

testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))

Test / test := (Test / test)
  .dependsOn(Compile / scalafmtCheck)
  .dependsOn(Test / scalafmtCheck)
  .value
