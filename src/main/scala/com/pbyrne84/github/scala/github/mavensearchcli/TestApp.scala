package com.pbyrne84.github.scala.github.mavensearchcli

import com.pbyrne84.github.scala.github.mavensearchcli.config.{ModuleConfig, ScalaVersion213}
import com.pbyrne84.github.scala.github.mavensearchcli.maven.MavenOrgSearchResult
import com.pbyrne84.github.scala.github.mavensearchcli.maven.client.{
  MavenSearchClient,
  MavenSingleSearch,
  NowProvider,
  SearchParams
}
import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault}

object TestApp extends ZIOAppDefault {

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    val days = 365
    val maybeDays = Some(days * 24 * 60 * 60)
    MavenSearchClient
      .searchOrg(
        SearchParams(
          orgName = "dev.zio",
          moduleConfig = ModuleConfig("zio-aws-machinelearning"),
          scalaVersion = ScalaVersion213,
          versionPattern = "(\\d+|\\.)",
          maybeWithinSeconds = maybeDays,
          maxPagesToPaginate = 2,
          retryCount = 2
        )
      )
      .map { mavenOrgSearchResult: MavenOrgSearchResult =>
        println(mavenOrgSearchResult)
      }
      .provide(MavenSearchClient.layer, NowProvider.layer, MavenSingleSearch.layer)
  }

}
