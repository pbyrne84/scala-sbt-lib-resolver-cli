# scala-sbt-lib-resolver-cli

A command line utility to allow you to get all your favourite lib details in sbt
format easily. Technologies used includes Graal for native compiles, ZIO 2, STTP, Decline, Circe and Docker.

It can handle

1. Scala and Java libs and scope them to test if that is desired
2. Sbt Plugins
3. Compiler plugins

This is handled by the module type field in the module definition, the definitions can be

1. scala
2. scala-test
3. java
4. java-test
5. plugin
6. compiler-plugin

Implementation can be found
<https://github.com/pbyrne84/scala-sbt-lib-resolver-cli/blob/main/src/main/scala/com/pbyrne84/github/scala/github/mavensearchcli/config/ModuleType.scala>

**Example module config set to an sbt plugin**

```json
{
  "name": "assembly",
  "org": "com.eed3si9n",
  "modules": [
    {
      "name": "sbt-assembly",
      "moduleType": "plugin"
    }
  ]
}
```

## How it searches

The app uses https://search.maven.org/solrsearch/ to find the latest version of the
libraries. For non sbt plugin scala libs the version is appended to the module name as this
is how it is stored in the index. There is also a version regex which allows us to only
receive main releases.

There is also a retry setting as the search can be a bit bumpy.

## Command line args

|           | value                                                                                                          | Example                    |
|-----------|:---------------------------------------------------------------------------------------------------------------|----------------------------|
| --hotlist | name of the hot list in the json                                                                               | --hotlist zio-app          |
| --group   | name of a group of libraries tied to an organisation                                                           | --group circe              |
| --debug   | switches log level to debug                                                                                    | --debug                    |
| --config  | Either the path to the config from the executable root or internal for <br/>the built in one.                  | --config config.json       |
| --version | Enables switching from the default scala version of 2.13, handy if you want to see if libs are available in 3. | --version 3 (or 2.12,2.13) |

The configuration is read from the executable path so the executable can operate on the path easily.

## Examples

Adding **--debug** as an argument will enable debug level logging.

The config used for these examples can be found in [src/main/resources/config.json](src/main/resources/config.json).
It is enabled by passing **--config internal**.

### Defaults

The defaults are set in the defaults section of config

```json
{
  "defaults": {
    "productionVersionRegex": "([\\d\\.]+)",
    "hotList": "zio-app"
  }
}
```

This allows the command line to skip **hotlist** and **group** arguments and just defer to the hotList set here.

### Hot Lists

A hot list is just a group of module groups allowing quick lookups. I always forget a chunk of them until later.
Some things do not manually search that easily and can be hard to find.

Running **MavenSearchCliApp** with **--hotlist zio-app --config internal**

results in

```scala
"dev.zio" %% "zio" % "2.0.6"
"dev.zio" %% "zio-config-typesafe" % "3.0.7"
"dev.zio" %% "zio-logging-slf4j" % "2.1.8"
"io.circe" %% "circe-core" % "0.14.3"
"io.circe" %% "circe-generic" % "0.14.3"
"io.circe" %% "circe-parser" % "0.14.3"
"dev.zio" %% "zio-test" % "2.0.6" % Test
"dev.zio" %% "zio-test-sbt" % "2.0.6" % Test
"ch.qos.logback" % "logback-classic" % "1.4.5"
"net.logstash.logback" % "logstash-logback-encoder" % "7.2"
"com.github.tomakehurst" % "wiremock" % "2.27.2" % Test
addSbtPlugin( "com.timushev.sbt" % "sbt-rewarn" % "0.1.3" )
addSbtPlugin( "org.scalameta" % "sbt-scalafmt" % "2.4.6" )
addSbtPlugin( "org.scoverage" % "sbt-scoverage" % "2.0.5" )
addCompilerPlugin( "com.olegpy" % "better-monadic-for" % "0.3.1" )
```

The config section that drives this looks something like

```json
{
  "name": "zio-app",
  "refs": [
    "circe",
    "logback",
    "logstash-logback",
    "zio",
    "wiremock",
    "scoverage",
    "scalafmt",
    "sbt-rewarn",
    "better-monadic-for"
  ]
}
```

under the **hotLists** section. Each ref points to a module group.

### Module group

A module group is a group of libraries linked to an organisation which in term can be bound in a list to a hot list.

Running **MavenSearchCliApp** with **--group circe --config internal** will look up everything under that name so

```json
{
  "name": "circe",
  "org": "io.circe",
  "modules": [
    {
      "name": "circe-core"
    },
    {
      "name": "circe-parser"
    },
    {
      "name": "circe-generic"
    }
  ]
}
```

will look up "circe-core", "circe-parser", "circe-generic".

## Binaries

The binaries are built using the 22.3.r11-grl version of Graal Java. This allows executables to be built that do not
need
a jvm to run. Using Graal also allows java libraries to be used as well. Java libraries can cause problems with the
amount of reflection they can use as Graal will only include calculable paths but this can be mitigated if everything is
well tested. We can then fork the tests and add the agent to the test run.

```scala
fork := true
Test / javaOptions += "-agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image"
```

All the files in the [src/main/resources/META-INF/native-image](src/main/resources/META-INF/native-image) folder are
then
auto generated. There can be a lot of junk in them which can raise warnings. I have also played with filtering out
stuff post run. It is way easier to get tests to do the calls exercising things like reflection than manually doing it.
It is also repeatable.

### Current built binaries

Current build binaries can be found in the [binaries/](binaries/) directory. They are built by a GitHub action
[.github/workflows/ci.yml](.github/workflows/ci.yml).

#### Using Docker to generate an Ubuntu Linux build

The included [Dockerfile](Dockerfile) builds an Ubuntu image with all the dependencies installed.

```bash
SET 
docker build -t lib_resolver_vm .
docker run --name lib_resolver_compile --mount src="$(pwd)",target=/root/project_mount,type=bind -t -d lib_resolver_vm
docker exec -it lib_resolver_compile bash
```

Then navigate to **/root/project_root** and run [test_linux_build.sh](test_linux_build.sh). This will run the tests
which generates any changes to the native image config
in [src/main/resources/META-INF/native-image](src/main/resources/META-INF/native-image)
then build the fat jar which is used by the **native-image** call.

#### Building a binary for Linux or OSX

For Ubuntu linux and OSX you can use [sdkman](https://sdkman.io/) to install the base dependencies. For Ubuntu the
following
may need to be installed **build-essential libz-dev zlib1g-dev**. OSX maybe the odd duck and need different other
dependencies than the following core dependencies.

```bash
sdk install scalacli
sdk list java
sdk install java 22.3.r11-grl 
gu install native-image
```

And then

```bash
sbt testAndBuildAssembly
```

Will build the fat jar if you do not want to trust the one in the [binaries/](binaries/) directory. Personally I
wouldn't
as I could be anyone :)

After the tests have run generating any [native-image](src%2Fmain%2Fresources%2FMETA-INF%2Fnative-image) config changes
which have been bundled into the created fat jar a native executable can then be created

```bash
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
```

which can then be run with

```bash
./scala-sbt-lib-resolver-cli --hotlist test-single --config internal --debug
```

This will use the internal config, **test-single** is an alias I use when I want to test a single item
hotList.
