package com.pbyrne84.github.scala.github.mavensearchcli.maven.client

import com.pbyrne84.github.scala.github.mavensearchcli.config.{ModuleConfig, ScalaVersion213, ScalaVersion3}
import com.pbyrne84.github.scala.github.mavensearchcli.maven.{FoundMavenOrgSearchResult, MissingMavenOrgSearchResult}
import com.pbyrne84.github.scala.github.mavensearchcli.shared.BaseSpec
import com.pbyrne84.github.scala.github.mavensearchcli.shared.BaseSpec.SharedDeps
import com.pbyrne84.github.scala.github.mavensearchcli.shared.wiremock.MavenWireMock
import org.mockito.Mockito
import zio.{Scope, ZIO, ZLayer}
import zio.test.TestAspect.sequential
import zio.test._

import java.time.Instant

object MavenSearchClientSpec extends BaseSpec {
  import Mockito._
  private val nowProvider: NowProvider = mock(classOf[NowProvider])
  private val nowProviderMockLayer = ZLayer.succeed(nowProvider)

  override protected def reset: ZIO[MavenWireMock, Throwable, Unit] = {
    for {
      _ <- ZIO.attempt(Mockito.reset(nowProvider))
      _ <- super.reset
    } yield ()
  }

  override def spec: Spec[SharedDeps with TestEnvironment with Scope, Any] =
    suite("searchOrg")(
      test("should return no results when none are found for no time limit") {
        for {
          _ <- reset
          orgSearchTerm = "orgX"
          moduleConfig = ModuleConfig("no-results")
          scalaVersion = ScalaVersion3
          _ <- MavenWireMock.stubSearchOrg(
            generateOrgOnlyQuery(orgSearchTerm, moduleConfig.versionedName(scalaVersion)),
            List.empty
          )
          result <- MavenSearchClient.searchOrg(
            SearchParams(
              orgName = orgSearchTerm,
              moduleConfig = moduleConfig,
              scalaVersion = scalaVersion,
              versionPattern = ".*",
              maybeWithinSeconds = None,
              maxPagesToPaginate = 1,
              retryCount = 0
            )
          )
        } yield assertTrue(
          result == MissingMavenOrgSearchResult(orgSearchTerm, moduleConfig.versionedName(scalaVersion), List.empty)
        )
      },
      test(
        "should return result up to pagination when the version matches the pattern"
      ) {
        val expected = (1 to 3).map(generateResult).toList
        for {
          _ <- reset
          orgSearchTerm = "orgY"
          moduleConfig = ModuleConfig("some-results")
          scalaVersion = ScalaVersion213
          _ <- MavenWireMock.stubSearchOrg(
            generateOrgOnlyQuery(orgSearchTerm, moduleConfig.versionedName(scalaVersion)),
            expected
          )
          result <- MavenSearchClient.searchOrg(
            SearchParams(
              orgName = orgSearchTerm,
              moduleConfig = moduleConfig,
              scalaVersion = scalaVersion,
              versionPattern = ".*\\.(2|3)",
              maybeWithinSeconds = None,
              maxPagesToPaginate = 1,
              retryCount = 0
            )
          )
        } yield assertTrue(
          result == FoundMavenOrgSearchResult(RawSearchResult("org", "module-3", "version.3"), moduleConfig)
        )
      },
      test(
        "should return value for an sbt plugin even though it doesn't have scala version suffix"
      ) {
        val expected = (1 to 3).map(generateResult).toList
        for {
          _ <- reset
          orgSearchTerm = "orgY"
          moduleConfig = ModuleConfig("some-results", isSbtPlugin = true)
          scalaVersion = ScalaVersion213
          _ <- MavenWireMock.stubSearchOrg(
            generateOrgOnlyQuery(orgSearchTerm, moduleConfig.name),
            expected
          )
          result <- MavenSearchClient.searchOrg(
            SearchParams(
              orgName = orgSearchTerm,
              moduleConfig = moduleConfig,
              scalaVersion = scalaVersion,
              versionPattern = ".*\\.(2|3)",
              maybeWithinSeconds = None,
              maxPagesToPaginate = 1,
              retryCount = 0
            )
          )
        } yield assertTrue(
          result == FoundMavenOrgSearchResult(RawSearchResult("org", "module-3", "version.3"), moduleConfig)
        )
      },
      test("should return the results if that matches version pattern within time limit") {
        val expectedResults = (1 to 3).map(generateResult).toList
        for {
          _ <- reset
          currentEpochMillis = 300000
          _ = when(nowProvider.getNow)
            .thenReturn(ZIO.succeed(Instant.ofEpochMilli(currentEpochMillis)))
          orgSearchTerm = "orgA"
          moduleConfig = ModuleConfig("some-results")
          scalaVersion = ScalaVersion3
          expectedQuery = generateTimeLimitedOrgQuery(
            orgSearchTerm,
            moduleConfig.versionedName(scalaVersion),
            startTimeStampInMillis = 100000
          )
          _ <- MavenWireMock.stubSearchOrg(expectedQuery, expectedResults)
          result <- MavenSearchClient.searchOrg(
            SearchParams(
              orgName = orgSearchTerm,
              moduleConfig = moduleConfig,
              scalaVersion = scalaVersion,
              versionPattern = ".*",
              maybeWithinSeconds = Some(200),
              maxPagesToPaginate = 1,
              retryCount = 0
            )
          )
        } yield assertTrue(
          result == FoundMavenOrgSearchResult(RawSearchResult("org", "module-3", "version.3"), moduleConfig)
        )
      },
      test("should return results up to pagination boundary when there is a time limit") {
        val expected = (1 to 3).map(generateResult).toList
        for {
          _ <- reset
          currentEpochMillis = 400000L
          withinSeconds = 150
          orgSearchTerm = "orgA"
          moduleConfig = ModuleConfig("some-results")
          scalaVersion = ScalaVersion3
          _ = when(nowProvider.getNow)
            .thenReturn(ZIO.succeed(Instant.ofEpochMilli(currentEpochMillis)))
          expectedQuery = generateTimeLimitedOrgQuery(
            orgSearchTerm,
            moduleConfig.versionedName(scalaVersion),
            startTimeStampInMillis = currentEpochMillis - (withinSeconds * 1000)
          )
          _ <- MavenWireMock.stubSearchOrg(expectedQuery, expected)
          result <- MavenSearchClient.searchOrg(
            SearchParams(
              orgName = orgSearchTerm,
              moduleConfig = moduleConfig,
              scalaVersion = scalaVersion,
              versionPattern = "version\\.3",
              maybeWithinSeconds = Some(withinSeconds),
              maxPagesToPaginate = 1,
              retryCount = 0
            )
          )
        } yield assertTrue(
          result == FoundMavenOrgSearchResult(RawSearchResult("org", "module-3", "version.3"), moduleConfig)
        )
      },
      test("paginate across results when there is 1 entry passed the first page for no time limit") {
        val expected = (1 to 4).map(generateResult).toList
        for {
          _ <- reset
          orgSearchTerm = "orgB"
          moduleConfig = ModuleConfig("more-results")
          scalaVersion = ScalaVersion3
          _ <- MavenWireMock.stubSearchOrg(
            generateOrgOnlyQuery(orgSearchTerm, moduleConfig.versionedName(scalaVersion)),
            expected
          )
          result <- MavenSearchClient.searchOrg(
            SearchParams(
              orgName = orgSearchTerm,
              moduleConfig = moduleConfig,
              scalaVersion = scalaVersion,
              versionPattern = "version\\.4",
              maybeWithinSeconds = None,
              maxPagesToPaginate = 2,
              retryCount = 0
            )
          )
        } yield assertTrue(
          result == FoundMavenOrgSearchResult(RawSearchResult("org", "module-4", "version.4"), moduleConfig)
        )
      },
      test(
        "paginate across results when there is an entry up to the end boundary of the third page for no time limit"
      ) {
        val expected = (1 to 9).map(generateResult).toList
        for {
          _ <- reset
          orgSearchTerm = "orgC"
          moduleConfig = ModuleConfig("even-more-results")
          scalaVersion = ScalaVersion3
          _ <- MavenWireMock.stubSearchOrg(
            generateOrgOnlyQuery(orgSearchTerm, moduleConfig.versionedName(scalaVersion)),
            expected
          )
          result <- MavenSearchClient.searchOrg(
            SearchParams(
              orgName = orgSearchTerm,
              moduleConfig = moduleConfig,
              scalaVersion = scalaVersion,
              versionPattern = "version\\.(8|9)",
              maybeWithinSeconds = None,
              maxPagesToPaginate = 3,
              retryCount = 0
            )
          )
        } yield assertTrue(
          result == FoundMavenOrgSearchResult(RawSearchResult("org", "module-9", "version.9"), moduleConfig)
        )
      },
      test(
        "will halt on page limit before a result can be found so we do not denial of service large repos"
      ) {
        val expected = (1 to 9).map(generateResult).toList
        for {
          _ <- reset
          orgSearchTerm = "orgC"
          moduleConfig = ModuleConfig("even-more-results")
          scalaVersion = ScalaVersion3
          _ <- MavenWireMock.stubSearchOrg(
            generateOrgOnlyQuery(orgSearchTerm, moduleConfig.versionedName(scalaVersion)),
            expected
          )
          result <- MavenSearchClient.searchOrg(
            SearchParams(
              orgName = orgSearchTerm,
              moduleConfig = moduleConfig,
              scalaVersion = scalaVersion,
              versionPattern = "version\\.(8|9)",
              maybeWithinSeconds = None,
              maxPagesToPaginate = 2,
              retryCount = 0
            )
          )
        } yield assertTrue(
          result == MissingMavenOrgSearchResult(
            organisation = orgSearchTerm,
            moduleName = moduleConfig.versionedName(scalaVersion),
            foundPotentialVersions = List("version.6", "version.5", "version.4", "version.3", "version.2", "version.1")
          )
        )
      }
    ).provideSome[MavenWireMock](MavenSearchClient.layer, nowProviderMockLayer, MavenSingleSearch.layer) @@ sequential

  private def generateResult(index: Int) =
    RawSearchResult("org", s"module-$index", s"version.$index")

  private def generateOrgOnlyQuery(orgName: String, moduleName: String): String = {
    s"g:$orgName AND a:$moduleName"
  }

  private def generateTimeLimitedOrgQuery(orgName: String, moduleName: String, startTimeStampInMillis: Long): String = {
    s"g:$orgName AND a:$moduleName AND timestamp:[$startTimeStampInMillis TO *]"
  }

}
