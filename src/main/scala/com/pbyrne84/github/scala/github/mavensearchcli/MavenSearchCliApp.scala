package com.pbyrne84.github.scala.github.mavensearchcli

import com.pbyrne84.github.scala.github.mavensearchcli.config._
import com.pbyrne84.github.scala.github.mavensearchcli.maven.MavenOrgSearchResult
import com.pbyrne84.github.scala.github.mavensearchcli.maven.client.{
  MavenSearchClient,
  MavenSingleSearch,
  NowProvider,
  SearchParams
}
import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault}

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

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    import cats.implicits._
    val a: Opts[(String, String)] = (hotListCommandLine, configPathCommand).mapN { (hotList, config) =>
      (hotList, config)
    }

    for {
      rawArgs <- ZIOAppArgs.getArgs
      // better monadic for does not work with native
      parsedArgs <- ZIO.fromEither(Command("", "")(a).parse(rawArgs))
      (hotList, configOption) = parsedArgs
      searchConfiguration <- new SearchConfigReader().fromCommandLineValue(configOption)
      searchConfig <- ZIO.fromEither(SearchConfig.decodeFromString(searchConfiguration))
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
          .mapError(_ => s"Could not find hotList '$hotList'")
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
