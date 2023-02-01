name := "scala-sbt-lib-resolver-cli"

scalaVersion := "2.13.10"

val zioVersion = "2.0.5"
val zioLoggingVersion = "2.1.7"
val circeVersion = "0.14.3"

ThisBuild / assemblyMergeStrategy := {
  case "application.conf" => MergeStrategy.concat
  case PathList("module-info.class") => MergeStrategy.discard
  case PathList("META-INF", "versions", xs @ _, "module-info.class") => MergeStrategy.discard
  case x =>
    val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
    oldStrategy(x)
}

addCompilerPlugin("io.tryp" % "splain" % "1.0.1" cross CrossVersion.patch)
addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")

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

lazy val jarName = "scala-sbt-lib-resolver-cli.jar"

// Forking will allow the agent to run
fork := true
Test / javaOptions += "-agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image"

lazy val nativeImageProject = (project in file("."))
  .enablePlugins(NativeImagePlugin)
  .settings(
    Compile / mainClass := Some("com.pbyrne84.github.scala.github.mavensearchcli.MavenSearchCliApp"),
    assembly / assemblyJarName := jarName
  )

scalacOptions ++= Seq( // use ++= to add to existing options
  "-encoding",
  "utf8", // if an option takes an arg, supply it on the same line
  "-feature", // then put the next option on a new line for easy editing
  "-language:implicitConversions",
  "-language:existentials",
  "-unchecked",
  "-Xlint" // exploit "trailing comma" syntax so you can add an option without editing this line
)

//not to be used in ci, intellij has got a bit bumpy in the format on save
val formatAndTest =
  taskKey[Unit]("format all code then run tests, do not use on CI as any changes will not be committed")

formatAndTest := {
  (Test / test)
    .dependsOn(Compile / scalafmtAll)
    .dependsOn(Test / scalafmtAll)
}.value

val testAndBuildAssembly =
  taskKey[Unit]("run tests and build fat jar")

testAndBuildAssembly := {
  (assembly)
    .dependsOn(Test / test)
}.value

//coverage does not work on windows due to filepath issues
//coverageEnabled := false
