package com.pbyrne84.github.scala.github.mavensearchcli.service

import com.pbyrne84.github.scala.github.mavensearchcli.ZIOServiced
import com.pbyrne84.github.scala.github.mavensearchcli.commandline.LookupType
import com.pbyrne84.github.scala.github.mavensearchcli.config.{GroupConfig, ModuleConfig, ScalaVersion, SearchConfig}
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
      scalaVersion: ScalaVersion
  ): ZIO[NowProvider with MavenSingleSearch with MavenSearchClient with MavenSearchCliService, Throwable, List[
    MavenOrgSearchResult
  ]] = serviced(_.run(searchConfig, lookup, scalaVersion))
}

class MavenSearchCliService {

  def run(
      searchConfig: SearchConfig,
      lookup: LookupType,
      scalaVersion: ScalaVersion
  ): ZIO[NowProvider with MavenSingleSearch with MavenSearchClient, Throwable, List[MavenOrgSearchResult]] = {
    for {
      _ <- ZIO.attempt(println(s"current lookup is ${lookup}"))
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
      scalaVersion: ScalaVersion
  ): ZIO[NowProvider with MavenSingleSearch with MavenSearchClient, Throwable, List[MavenOrgSearchResult]] = {
    val days = 365
    val maybeWithinSeconds = None // Some(days * 24 * 60 * 60)

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
