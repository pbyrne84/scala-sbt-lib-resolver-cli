package com.pbyrne84.github.scala.github.mavensearchcli.service

import com.pbyrne84.github.scala.github.mavensearchcli.ZIOServiced
import com.pbyrne84.github.scala.github.mavensearchcli.commandline.LookupType
import com.pbyrne84.github.scala.github.mavensearchcli.config.{
  GroupConfig,
  ModuleConfig,
  SearchConfig,
  ValidScalaVersion
}
import com.pbyrne84.github.scala.github.mavensearchcli.error.CliException
import com.pbyrne84.github.scala.github.mavensearchcli.maven.MavenOrgSearchResult
import com.pbyrne84.github.scala.github.mavensearchcli.maven.client.{
  MavenSearchClient,
  MavenSingleSearch,
  NowProvider,
  SearchParams
}
import zio.{ZIO, ZLayer}

object MavenSearchCliService extends ZIOServiced[MavenSearchCliService] {
  val layer: ZLayer[Any, Throwable, MavenSearchCliService] = ZLayer {
    ZIO.succeed(new MavenSearchCliService)
  }

  def run(
      searchConfig: SearchConfig,
      lookup: LookupType,
      scalaVersion: ValidScalaVersion
  ): ZIO[NowProvider with MavenSingleSearch with MavenSearchClient with MavenSearchCliService, CliException, List[
    MavenOrgSearchResult
  ]] = serviced(_.run(searchConfig, lookup, scalaVersion))
}

class MavenSearchCliService {

  def run(
      searchConfig: SearchConfig,
      lookup: LookupType,
      scalaVersion: ValidScalaVersion
  ): ZIO[NowProvider with MavenSingleSearch with MavenSearchClient, CliException, List[MavenOrgSearchResult]] = {
    for {
      _ <- ZIO.succeed(println(s"current lookup is ${lookup}"))
      orgConfigs <- ZIO.fromEither(lookup.getOrgConfigsForLookup(searchConfig))
      results <- ZIO
        .foreach(orgConfigs) { orgConfig =>
          searchUsingConfig(searchConfig, orgConfig, scalaVersion)
        }
        .map(_.flatten)
    } yield results
  }

  private def searchUsingConfig(
      searchConfig: SearchConfig,
      orgConfig: GroupConfig,
      scalaVersion: ValidScalaVersion
  ): ZIO[NowProvider with MavenSingleSearch with MavenSearchClient, CliException, List[MavenOrgSearchResult]] = {
    val maybeWithinSeconds = None

    ZIO
      .foreach(orgConfig.modules) { module: ModuleConfig =>
        val searchParams = SearchParams(
          orgName = orgConfig.org,
          moduleConfig = module,
          scalaVersion = scalaVersion,
          searchConfig.defaultProductionVersionRegex,
          maybeWithinSeconds = maybeWithinSeconds,
          maxPagesToPaginate = searchConfig.maximumPagesToPaginate,
          retryCount = searchConfig.retryCount
        )

        MavenSearchClient.searchOrg(searchParams)
      }

  }

}
