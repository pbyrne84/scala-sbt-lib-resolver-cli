package com.pbyrne84.github.scala.github.mavensearchcli

import com.pbyrne84.github.scala.github.mavensearchcli.config.{ModuleConfig, OrgConfig, ScalaVersion213, SearchConfig}
import com.pbyrne84.github.scala.github.mavensearchcli.maven.MavenOrgSearchResult
import com.pbyrne84.github.scala.github.mavensearchcli.maven.client.{
  MavenSearchClient,
  MavenSingleSearch,
  NowProvider,
  SearchParams
}
import com.typesafe.config.impl.ConfigImpl
import com.typesafe.config.{Config, ConfigFactory}
import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault}

import java.io.File

object MavenSearchCliApp extends ZIOAppDefault {

  import com.monovore.decline._

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

  private val orgCommandLine = Opts.option[String](
    long = "org",
    short = "o",
    help =
      "An org is a reference to libraries under an org, the name is separate so you can have many configs for an org"
  )

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    for {
      args <- ZIOAppArgs.getArgs
      hotList <- ZIO.fromEither(Command("", "")(hotListCommandLine).parse(args))
      searchConfig <- SearchConfig.readFromResource("config.json")
      searchResults <- new MavenSearchCliApp()
        .run(searchConfig, hotList)
        .provide(MavenSearchClient.layer, NowProvider.layer, MavenSingleSearch.layer)
      _ = searchResults
        .sortBy(MavenOrgSearchResult.comparable)
        .foreach(result => println(result.render))
    } yield println(searchResults)
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
          .mapError(error => s"Could not find hotList '$hotList'")
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
