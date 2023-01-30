package com.pbyrne84.github.scala.github.mavensearchcli

import ch.qos.logback.classic
import ch.qos.logback.classic.LoggerContext
import com.pbyrne84.github.scala.github.mavensearchcli.commandline.CommandLineArgs
import com.pbyrne84.github.scala.github.mavensearchcli.config._
import com.pbyrne84.github.scala.github.mavensearchcli.maven.MavenOrgSearchResult
import com.pbyrne84.github.scala.github.mavensearchcli.maven.client.{MavenSearchClient, MavenSingleSearch, NowProvider}
import com.pbyrne84.github.scala.github.mavensearchcli.service.MavenSearchCliService
import org.slf4j.LoggerFactory
import zio.logging.backend.SLF4J
import zio.{Scope, Task, ZIO, ZIOAppArgs, ZIOAppDefault}

import scala.jdk.CollectionConverters.CollectionHasAsScala

object MavenSearchCliApp extends ZIOAppDefault {

  private val loggingLayer = zio.Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    (for {
      rawArgs <- ZIOAppArgs.getArgs
      parsedArgs <- ZIO.fromEither(CommandLineArgs.fromCliArgs(rawArgs.toList))
      _ <- initialiseLogLevel(parsedArgs.enableDebug)
      searchResults <- executeUsingArgs(parsedArgs)
      _ <- ZIO.logDebug(searchResults.toString)
    } yield ()).provideSome[ZIOAppArgs](loggingLayer)
  }

  private def initialiseLogLevel(enableDebug: Boolean): Task[Unit] = {
    LoggerFactory.getILoggerFactory match {
      case context: LoggerContext =>
        val logLevel =
          if (enableDebug)
            classic.Level.DEBUG
          else
            classic.Level.INFO

        ZIO.attempt(context.getLoggerList.asScala.foreach { logger =>
          logger.setLevel(logLevel)
        })

      case other =>
        ZIO.fail(new RuntimeException(s"Unexpected logger factory response $other"))
    }

  }

  private def executeUsingArgs(commandLineArgs: CommandLineArgs): ZIO[Any, Throwable, List[MavenOrgSearchResult]] = {
    for {
      searchConfiguration <- new SearchConfigReader().fromCommandLineValue(commandLineArgs.configOption)
      searchConfig <- ZIO.fromEither(SearchConfig.decodeFromString(searchConfiguration))
      searchResults <- MavenSearchCliService
        .run(searchConfig, commandLineArgs.lookup, ScalaVersion213)
        .provide(MavenSearchClient.layer, NowProvider.layer, MavenSingleSearch.layer, MavenSearchCliService.layer)
      _ = searchResults
        .sortBy(MavenOrgSearchResult.comparable)
        .foreach(result => println(result.render))
    } yield searchResults

  }
}
