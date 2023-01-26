package com.pbyrne84.github.scala.github.mavensearchcli

import com.pbyrne84.github.scala.github.mavensearchcli.config._
import com.pbyrne84.github.scala.github.mavensearchcli.maven.MavenOrgSearchResult
import com.pbyrne84.github.scala.github.mavensearchcli.maven.client.{
  MavenSearchClient,
  MavenSingleSearch,
  NowProvider,
  SearchParams
}
import zio.logging.backend.SLF4J
import zio.{LogLevel, Scope, ZIO, ZIOAppArgs, ZIOAppDefault}

object MavenSearchCliApp extends ZIOAppDefault {

  import com.monovore.decline._

  private val loggingLayer = zio.Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  private val hotListCommandLine = Opts.option[String](
    long = "hotlist",
    short = "h",
    help = "A hotList is a list of configured references you want to group, more than one can be comma seperated"
  )

  private val configPathCommand = Opts.option[String](
    long = "config",
    short = "c",
    help = "Path of the config file"
  )

  private val enableDebugCommand = Opts
    .flag(
      long = "debug",
      short = "d",
      help = "view debug lugs"
    )
    .map(_ => true)
    .withDefault(false)

  private case class CommandLineArgs(hotList: String, configOption: String, enableDebug: Boolean)

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    import cats.implicits._
    val commandLineArgs: Opts[CommandLineArgs] = (hotListCommandLine, configPathCommand, enableDebugCommand).mapN {
      (hotList, config, enableDebug) =>
        CommandLineArgs(hotList, config, enableDebug)
    }

    (for {
      rawArgs <- ZIOAppArgs.getArgs
      parsedArgs <- ZIO.fromEither(Command("", "")(commandLineArgs).parse(rawArgs))
      loglevel = calculateLogLevel(parsedArgs.enableDebug)
      searchResults <- executeUsingArgs(loglevel, parsedArgs)
    } yield println(searchResults)).provideSome[ZIOAppArgs](loggingLayer)
  }

  private def calculateLogLevel(enableDebug: Boolean): LogLevel = {
    ZIO.logLevel {
      if (enableDebug) LogLevel.Debug else LogLevel.Info
    }
  }

  private def executeUsingArgs(logLevel: LogLevel, commandLineArgs: CommandLineArgs) = {
    ZIO.logLevel(logLevel) {
      for {
        searchConfiguration <- new SearchConfigReader().fromCommandLineValue(commandLineArgs.configOption)
        searchConfig <- ZIO.fromEither(SearchConfig.decodeFromString(searchConfiguration))
        searchResults <- new MavenSearchCliApp()
          .run(searchConfig, commandLineArgs.hotList)
          .provide(MavenSearchClient.layer, NowProvider.layer, MavenSingleSearch.layer)
        _ = searchResults
          .sortBy(MavenOrgSearchResult.comparable)
          .foreach(result => println(result.render))
      } yield println(searchResults)
    }
  }
}

class MavenSearchCliApp {

  def run(searchConfig: SearchConfig, hotList: String) = {
    for {
      results <- for {
        hotListMappings <- ZIO.fromEither(searchConfig.errorOrHotListMappings)
        _ = println(s"hotList $hotList")
        hotListConfig <- ZIO
          .fromOption(hotListMappings.get(hotList))
          .mapError(_ => s"Could not find hotList '$hotList' in ${hotListMappings.map(_._1).mkString(", ")}")
        results <- ZIO
          .foreach(hotListConfig) { orgConfig =>
            searchUsingConfig(searchConfig, orgConfig)
          }
          .map(_.flatten)
      } yield results
      _ = results

    } yield results

  }

  private def searchUsingConfig(
      searchConfig: SearchConfig,
      orgConfig: OrgConfig
  ): ZIO[NowProvider with MavenSingleSearch with MavenSearchClient, Throwable, List[MavenOrgSearchResult]] = {
    val days = 365
    val maybeWithinSeconds = None // Some(days * 24 * 60 * 60)

    ZIO
      .foreach(orgConfig.modules) { module: ModuleConfig =>
        val searchParams = SearchParams(
          orgName = orgConfig.org,
          moduleConfig = module,
          scalaVersion = ScalaVersion213,
          searchConfig.defaultProductionVersionRegex,
          maybeWithinSeconds = maybeWithinSeconds,
          maxPagesToPaginate = searchConfig.maximumPagesToPaginate,
          retryCount = searchConfig.retryCount
        )

        MavenSearchClient.searchOrg(searchParams)
      }

  }

}
