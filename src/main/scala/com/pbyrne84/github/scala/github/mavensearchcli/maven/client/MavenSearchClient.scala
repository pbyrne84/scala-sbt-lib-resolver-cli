package com.pbyrne84.github.scala.github.mavensearchcli.maven.client

import com.pbyrne84.github.scala.github.mavensearchcli.ZIOServiced
import com.pbyrne84.github.scala.github.mavensearchcli.config.{CommandLineConfig, ModuleConfig, ValidScalaVersion}
import com.pbyrne84.github.scala.github.mavensearchcli.error.CliException
import com.pbyrne84.github.scala.github.mavensearchcli.maven.{
  FoundMavenOrgSearchResult,
  MavenOrgSearchResult,
  MavenOrgSearchResults,
  MissingMavenOrgSearchResult
}
import zio.config.ReadError
import zio.{ZIO, ZLayer}

sealed abstract class MavenSearchClientError(message: String, maybeCause: Option[Throwable])
    extends RuntimeException(message, maybeCause.orNull)

case class UnExpectedMavenResponseError(message: String, maybeCause: Option[Throwable] = None)
    extends MavenSearchClientError(message, maybeCause)

case class SearchParams(
    orgName: String,
    moduleConfig: ModuleConfig,
    scalaVersion: ValidScalaVersion,
    versionPattern: String,
    maybeWithinSeconds: Option[Int],
    maxPagesToPaginate: Int,
    retryCount: Int
) {
  val versionedModuleName: String = moduleConfig.scalaVersionedName(scalaVersion)

}

object MavenSearchClient extends ZIOServiced[MavenSearchClient] {
  val layer: ZLayer[Any, ReadError[String], MavenSearchClient] = ZLayer {
    for {
      config <- ZIO.service[CommandLineConfig].provide(CommandLineConfig.layer)
    } yield new MavenSearchClient(config.mavenPageSize)
  }

  def searchOrg(
      searchParams: SearchParams
  ): ZIO[NowProvider with MavenSingleSearch with MavenSearchClient, CliException, MavenOrgSearchResult] =
    serviced(_.searchOrg(searchParams)).retryN(6)

}

class MavenSearchClient(pageSize: Int) {

  private implicit val versionOrdering: Ordering[RawSearchResult] = new Ordering[RawSearchResult] {
    override def compare(x: RawSearchResult, y: RawSearchResult): Int = {
      x.version compare y.version
    }
  }.reverse

  def searchOrg(
      searchParams: SearchParams
  ): ZIO[NowProvider with MavenSingleSearch, CliException, MavenOrgSearchResult] = {
    val startIndex = 0
    for {
      _ <- ZIO.logDebug(s"searching maven using $searchParams")
      headSearchResult <- MavenSingleSearch.runQuery(
        searchParams,
        startIndex = startIndex
      )
      versionPatternFilteredHeadSearchResult = headSearchResult.pagedResults
        .filter(_.version.matches(searchParams.versionPattern))
        .sorted
        .headOption
      finalSearchResult <- maybePaginatePastFirstPage(
        headSearchResult,
        versionPatternFilteredHeadSearchResult,
        searchParams
      )
      _ <- ZIO.logDebug(s"$searchParams resolved to $finalSearchResult")
    } yield finalSearchResult
  }

  private def maybePaginatePastFirstPage(
      searchResults: MavenOrgSearchResults,
      maybeFirstPageSearchResult: Option[RawSearchResult],
      searchParams: SearchParams
  ): ZIO[NowProvider with MavenSingleSearch, CliException, MavenOrgSearchResult] = {
    maybeFirstPageSearchResult match {
      case Some(value) =>
        ZIO.succeed(FoundMavenOrgSearchResult(value, searchParams.moduleConfig))

      case None =>
        val pagesAvailable = (searchResults.totalFound.toFloat / pageSize).ceil.toInt
        paginateUntilFound(
          searchParams = searchParams,
          pagesAvailable = pagesAvailable,
          currentPage = 2,
          foundVersions = searchResults.pagedResults.sorted.map(_.version)
        )
    }
  }

  private def paginateUntilFound(
      searchParams: SearchParams,
      pagesAvailable: Int,
      currentPage: Int,
      foundVersions: List[String]
  ): ZIO[NowProvider with MavenSingleSearch, CliException, MavenOrgSearchResult] = {
    val versionModuleName = searchParams.versionedModuleName

    if (pagesAvailable == 0 || currentPage > pagesAvailable || currentPage > searchParams.maxPagesToPaginate) {
      ZIO.succeed(MissingMavenOrgSearchResult(searchParams.orgName, versionModuleName, foundVersions.sorted.reverse))
    } else {
      val currentPageOffset = (currentPage - 1) * pageSize
      MavenSingleSearch
        .runQuery(
          searchParams,
          currentPageOffset
        )
        .flatMap { results =>
          val pagedResults = results.pagedResults
          if (pagedResults.isEmpty) {
            ZIO.succeed(MissingMavenOrgSearchResult(searchParams.orgName, versionModuleName, foundVersions))
          } else {
            val filteredResults = pagedResults.filter(_.version.matches(searchParams.versionPattern))
            filteredResults.sorted match {
              case ::(head, _) =>
                ZIO.succeed(FoundMavenOrgSearchResult(head, searchParams.moduleConfig))
              case Nil =>
                paginateUntilFound(
                  searchParams,
                  pagesAvailable,
                  currentPage + 1,
                  foundVersions ++ results.pagedResults.sorted.map(_.version)
                )
            }

          }
        }
    }
  }

}
