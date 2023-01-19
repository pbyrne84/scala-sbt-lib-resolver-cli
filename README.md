# scala-sbt-lib-resolver-cli

A command line utility to allow you to get all your favourite lib details in sbt 
format easily.

## How it searches

The app uses https://search.maven.org/solrsearch/ to find the latest version of the 
libraries. For non sbt plugin scala libs the version is appended to the module name as this
is how it is stored in the index. There is also a version regex which allows us to only
receive main releases.

There is also a retry setting as the search can be a bit bumpy.

## Example

Running **MavenSearchCliApp** with **--hotlist zio-app**

results in 

```scala
"ch.qos.logback" % "logback-classic" % "1.4.5"
"dev.zio" %% "zio" % "2.0.6"
"dev.zio" %% "zio-config-typesafe" % "3.0.7"
"dev.zio" %% "zio-logging-slf4j" % "2.1.7"
"io.circe" %% "circe-core" % "0.14.3"
"io.circe" %% "circe-generic" % "0.14.3"
"io.circe" %% "circe-parser" % "0.14.3"
"net.logstash.logback" % "logstash-logback-encoder" % "7.2"
"com.github.tomakehurst" % "wiremock" % "2.27.2" % Test
"dev.zio" %% "zio-test" % "2.0.6" % Test
"dev.zio" %% "zio-test-sbt" % "2.0.6" % Test
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.6")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.5")
```

The config can be found in [src/main/resources/config.json](src/main/resources/config.json)


## Todo

1. Allow easy lookup of modules using there alias, for example 
  ```json
   {
      "name": "assembly",
      "org": "com.eed3si9n",
      "modules": [
        {
          "name": "sbt-assembly",
          "isSbtPlugin" : true
        }
      ]
    }
  ```
  should allow us to do **--lib assembly** as a shorthand to just get the latest lib.

2. Switch to graal and build native executables and make things more user-friendly.

3. Allow config to be yaml as well for the enablement of unwieldy whitespace issues in
   large files. The config files have the potential to get very big.
