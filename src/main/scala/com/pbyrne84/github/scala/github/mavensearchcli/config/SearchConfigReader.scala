package com.pbyrne84.github.scala.github.mavensearchcli.config

import com.pbyrne84.github.scala.github.mavensearchcli.config.SearchConfigReader.{
  defaultCommandLineOption,
  defaultFileName
}
import com.pbyrne84.github.scala.github.mavensearchcli.error.ConfigReaderException
import zio.ZIO

import java.io.File
import scala.io.Source
import scala.util.Using

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
      ZIO.logDebug("attempting to read internal default config") *> readDefault.mapError(error =>
        ConfigReaderException(s"The default internal resource $defaultFileName could not be read", error)
      )
    } else {
      ZIO.logDebug(s"attempting to read custom config '$value'") *> readCustom(value, runSource).mapError(error =>
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

  /** This will attempt to calculate the config path relative to the executable, not where the command line is
    * currently. This enables the executable to be put on the path and run from anywhere.
    * @param value
    * @param runSource
    * @return
    */
  private def readCustom(value: String, runSource: Class[_]): ZIO[Any, Throwable, String] = {
    for {
      executableBasePath <- calculateExecutableBasePath(value, runSource)
      filePath = s"$executableBasePath/$value"
      fileContents <- ZIO.fromEither(Using(Source.fromFile(filePath)) { reader =>
        reader.mkString
      }.toEither.left.map(error => new RuntimeException(s"Could not read config from $filePath", error)))
    } yield {
      fileContents
    }

  }

  private def calculateExecutableBasePath(value: String, runSource: Class[_]): ZIO[Any, Throwable, String] = {
    // Once in executable form we cannot debug easily hence the verbosity
    for {
      _ <- ZIO.logDebug(s"calculating custom config path from $value")
      protectionDomain <- ZIO.attempt(runSource.getProtectionDomain)
      _ <- ZIO.logDebug(s"current protectionDomain $protectionDomain")
      codeSource <- ZIO.attempt(protectionDomain.getCodeSource)
      _ <- ZIO.logDebug(s"current codeSource $codeSource")
      location <- ZIO.attempt(codeSource.getLocation)
      _ <- ZIO.logDebug(s"current code location $location")
      _ <- ZIO.logDebug(s"current code location.toUri ${location.toURI}")
      _ <- ZIO.logDebug(s"current code location.toUri.getPath ${location.toURI.getPath}")
      runningFileLocation <- ZIO.attempt(new File(location.toURI.getPath))
      // so when we run the test the folder is returned but when it is a native image it is the executable
      // meaning we then need the parent. No way to really test that
      executableBasePath <- calculateExecutableParentFolder(runningFileLocation)
    } yield {
      executableBasePath
    }
  }

  /** When running from sbt the absolute path will be the root folder of the project, where as if when this is converted
    * to an executable the absolute path will be the executable hence we need to to the parent.
    * @param runningFileLocation
    * @return
    */
  private def calculateExecutableParentFolder(runningFileLocation: File) = {
    for {
      _ <- ZIO.logDebug(s"calculating save executable location from $runningFileLocation")
      safeLocation = (if (runningFileLocation.isFile) {
                        runningFileLocation.getParent
                      } else {
                        runningFileLocation.getAbsolutePath
                      })
        // windows does not really usually care about \ being /
        .replace("\\", "/")
        .stripSuffix("/")
      _ <- ZIO.logDebug(s"custom config location base path is $safeLocation")
    } yield safeLocation

  }
}

object SearchConfigReader {
  val defaultCommandLineOption: String = "internal"
  private val defaultFileName = "config.json"

}
