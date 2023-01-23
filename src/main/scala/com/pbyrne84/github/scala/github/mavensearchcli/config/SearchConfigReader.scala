package com.pbyrne84.github.scala.github.mavensearchcli.config

import com.pbyrne84.github.scala.github.mavensearchcli.config.SearchConfigReader.{
  defaultCommandLineOption,
  defaultFileName,
  getClass
}
import zio.{IO, ZIO}

import java.io.File
import scala.io.Source
import scala.util.{Try, Using}

case class ConfigReaderException(message: String, cause: Throwable) extends RuntimeException(message, cause)

class SearchConfigReader {

  /** @param value
    *   \- this can be the default command line option or a value from the operation source route
    * @param runSource
    *   \- this exists so we can run from the test src path so we do not muddy the production dir
    * @return
    */
  def fromCommandLineValue(
      value: String,
      runSource: Class[_] = SearchConfigReader.getClass
  ): ZIO[Any, ConfigReaderException, String] = {
    if (value == defaultCommandLineOption) {
      readDefault.mapError(error =>
        ConfigReaderException(s"The default internal resource $defaultFileName could not be read", error)
      )
    } else {
      readCustom(value, runSource).mapError(error =>
        ConfigReaderException(s"The custom location $value could not be read", error)
      )
    }
  }

  private def readDefault = {
    ZIO.fromEither {
      for {
        // getResourceAsStream works when something is packaged and we want something from the internals of the package
        stream <- Option(getClass.getClassLoader.getResourceAsStream(defaultFileName)).toRight(
          new RuntimeException(s"'$defaultFileName' could not be resolved using the internal resource as stream method")
        )
        contents <- Using(scala.io.Source.fromInputStream(stream)) { reader =>
          reader.mkString
        }.toEither
      } yield {
        contents
      }
    }
  }

  private def readCustom(value: String, runSource: Class[_]): IO[Throwable, String] = {
    ZIO.fromEither {
      val errorOrExecutableBasePath = Try {
        val protectionDomain = runSource.getProtectionDomain
        println(s"protectionDomain $protectionDomain")

        val codeSource = protectionDomain.getCodeSource
        println(s"codeSource $codeSource")

        val location = codeSource.getLocation
        // println(s"location $location")
        println(s"location.toURI ${location.toURI}")
        println(s"location.toURI.getPath ${location.toURI.getPath}")

        val currentLocation = new File(location.toURI.getPath)
        // so when we run the test the folder is returned but when it is a native image it is the executable
        // meaning we then need the parent. No way to really test that
        if (currentLocation.isFile) {
          currentLocation.getParent
        } else {
          currentLocation.getAbsolutePath
        }
      }.toEither.map {
        // windows does not really usually care about \ being /
        _.replace("\\", "/").stripSuffix("/")
      }
      for {
        executableBasePath <- errorOrExecutableBasePath
        filePath = s"$executableBasePath/$value"
        fileContents <- Using(Source.fromFile(filePath)) { reader =>
          reader.mkString
        }.toEither.left.map(error => new RuntimeException(s"Could not read config from $filePath", error))
      } yield {
        fileContents
      }
    }
  }
}

object SearchConfigReader {
  val defaultCommandLineOption: String = "internal"
  private val defaultFileName = "config.json"

}
