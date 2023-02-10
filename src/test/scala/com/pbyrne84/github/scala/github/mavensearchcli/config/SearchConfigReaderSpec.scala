package com.pbyrne84.github.scala.github.mavensearchcli.config

import com.pbyrne84.github.scala.github.mavensearchcli.error.ConfigReaderException
import com.pbyrne84.github.scala.github.mavensearchcli.shared.BaseSpec
import com.pbyrne84.github.scala.github.mavensearchcli.shared.BaseSpec.SharedDeps
import zio.test.Assertion._
import zio.test.TestAspect.sequential
import zio.test._
import zio.{IO, Scope, ZIO}

import scala.io.Source
import scala.util.{Try, Using}

object SearchConfigReaderSpec extends BaseSpec {
  private val searchConfigReader: SearchConfigReader = new SearchConfigReader()

  override def spec: Spec[SharedDeps with TestEnvironment with Scope, Any] =
    suite("SearchConfigReaderSpec.fromCommandLineValue")(
      test("should return the default config contents when passed the default option which is internal") {
        val fileName = "config.json"

        for {
          expected <- readResource(fileName)
          defaultFileContents <- searchConfigReader.fromCommandLineValue(SearchConfigReader.defaultCommandLineOption)
        } yield assertTrue(defaultFileContents == expected)
      },
      // We want to be able to read from the executable directory when packaged and not the cli location
      // as that may likely be the most common organisation
      test(
        "should return contents of a custom location when not default and resolution can be relative to the jar/executable"
      ) {
        for {
          // D:\development\scala-sbt-lib-resolver-cli\src\test\scala\empty_config.json
          expected <- readFile("src/test/resources/empty_config.json")
          actual <- searchConfigReader.fromCommandLineValue("empty_config.json", SearchConfigReaderSpec.getClass)
        } yield assertTrue(actual == expected)

      },
      test(
        "should fail gracefully when it cannot be read"
      ) {
        // For errors typed errors make things easy, testing error messages less useful.
        val actual = searchConfigReader.fromCommandLineValue("missing.json", SearchConfigReaderSpec.getClass)
        assertZIO(actual.mapError(_.getClass).exit)(fails(equalTo(classOf[ConfigReaderException])))
      }
    ) @@ sequential

  private def readResource(file: String): IO[RuntimeException, String] = {
    for {
      resourceFilePath <- ZIO.fromEither(
        Try(getClass.getClassLoader.getResource(file).getFile).toEither.left
          .map(error => new RuntimeException(s"$file could not resolve to a resource", error))
      )
      resourceFileContents <- readFile(resourceFilePath)
    } yield resourceFileContents
  }

  private def readFile(fullPath: String): IO[RuntimeException, String] = {
    ZIO.fromEither(
      Using(Source.fromFile(fullPath)) { reader =>
        reader.mkString
      }.toEither.left.map(error => new RuntimeException(s"Could not read from $fullPath", error))
    )
  }

}
