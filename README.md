# scala-sbt-lib-resolver-cli

A command line utility to allow you to get all your favourite lib details in sbt 
format easily.

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
          "moduleType" : "plugin"
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

## Examples

Adding **--debug** as an argument will enable debug level logging.

### Hot Lists
A hot list is just a group of module groups allowing quick lookups. I always forget a chunk of them until later.
Some things do not manually search that easily and can be hard to find.

Running **MavenSearchCliApp** with **--hotlist zio-app**

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
addSbtPlugin("com.timushev.sbt" % "sbt-rewarn" % "0.1.3")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.6")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.5")
addCompilerPlugin("com.olegpy" % "better-monadic-for" % "0.3.1")
```

The config can be found in [src/main/resources/config.json](src/main/resources/config.json)

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

Running **MavenSearchCliApp** with **--group circe** will look up everything under that name so 

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

