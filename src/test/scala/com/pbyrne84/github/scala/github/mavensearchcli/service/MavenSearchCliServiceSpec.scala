package com.pbyrne84.github.scala.github.mavensearchcli.service

import cats.data.NonEmptyList
import com.pbyrne84.github.scala.github.mavensearchcli.commandline.{CustomHotListLookupType, ModuleGroupLookupType}
import com.pbyrne84.github.scala.github.mavensearchcli.config._
import com.pbyrne84.github.scala.github.mavensearchcli.error.MissingHotListException
import com.pbyrne84.github.scala.github.mavensearchcli.maven.client.{
  MavenSearchClient,
  MavenSingleSearch,
  NowProvider,
  RawSearchResult
}
import com.pbyrne84.github.scala.github.mavensearchcli.maven.{FoundMavenOrgSearchResult, MavenOrgSearchResult}
import com.pbyrne84.github.scala.github.mavensearchcli.shared.BaseSpec
import com.pbyrne84.github.scala.github.mavensearchcli.shared.BaseSpec.SharedDeps
import com.pbyrne84.github.scala.github.mavensearchcli.shared.wiremock.MavenWireMock
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._
import zio.{Scope, ZIO}

object MavenSearchCliServiceSpec extends BaseSpec {
  override def spec: Spec[SharedDeps with TestEnvironment with Scope, Any] = {
    suite(getClass.getSimpleName)(
      suite("run")(
        test("should process hotList successfully and find all types") {
          for {
            _ <- reset
            config = createSearchConfig
            hostListName = "hotlist-1"
            scalaVersion = ScalaVersion213
            stubbedResults <- stubWireMockForHotList(scalaVersion, config, hotListName = hostListName)
            actualResults <- MavenSearchCliService
              .run(config, CustomHotListLookupType(hostListName), scalaVersion)
              .flatMap { results =>
                ZIO.fromEither(results.asNonEmptyList)
              }
          } yield assertTrue(actualResults == stubbedResults && actualResults.size == 12)
        },
        test("should process org lookup successfully and find all types") {
          for {
            _ <- reset
            config = createSearchConfig
            orgConfigReferenceName = "lib-2-ref"
            scalaVersion = ScalaVersion213
            stubbedResults <- stubWireMockForOrgConfig(
              scalaVersion,
              config,
              orgConfigReferenceName = orgConfigReferenceName
            )
            actualResults <- MavenSearchCliService
              .run(config, ModuleGroupLookupType(orgConfigReferenceName), scalaVersion)
              .flatMap { results =>
                ZIO.fromEither(results.asNonEmptyList)
              }
          } yield assertTrue(actualResults == stubbedResults && actualResults.size == 6)
        },
        test("should error when hotList is not found") {
          for {
            _ <- reset
            error <- MavenSearchCliService
              .run(createSearchConfig, CustomHotListLookupType("unknown"), ScalaVersion213)
              .flip
          } yield assert(error)(isSubtype[MissingHotListException](Assertion.anything))
        }
      )
    ).provideSome[SharedDeps](
      MavenSearchCliService.layer,
      NowProvider.layer,
      MavenSearchClient.layer,
      MavenSingleSearch.layer
    )
  } @@ sequential

  private def createSearchConfig: SearchConfig = {
    val hotListItemConfigs = (1 to 3).map { index =>
      HotListItemConfig(name = s"hotlist-$index", refs = List("lib-1-ref", "lib-2-ref"))
    }.toList

    val orgs = hotListItemConfigs.zipWithIndex.map { case (_, itemConfigIndex) =>
      val orgName = s"org-${itemConfigIndex % 2}"

      val modules = ModuleType.valueMap.values.zipWithIndex.map { case (moduleType, moduleTypeIndex) =>
        ModuleConfig(s"module-$itemConfigIndex-$moduleTypeIndex", None, moduleType)
      }.toList

      GroupConfig(s"lib-$itemConfigIndex-ref", org = orgName, modules = modules)
    }

    SearchConfig(
      defaults = ConfigDefaults(".*"),
      maximumPagesToPaginate = 5,
      hotLists = hotListItemConfigs,
      groups = orgs
    )
  }

  private def stubWireMockForHotList(
      scalaVersion: ValidScalaVersion,
      searchConfig: SearchConfig,
      hotListName: String
  ): ZIO[MavenWireMock, Throwable, NonEmptyList[MavenOrgSearchResult]] = {
    for {
      orgConfigs <- ZIO.fromEither(searchConfig.getHotListOrgConfigs(hotListName))
      searchResults <- createStubbingsForOrgConfigs(scalaVersion, orgConfigs)
    } yield searchResults
  }

  private def createStubbingsForOrgConfigs(
      scalaVersion: ValidScalaVersion,
      orgConfigs: List[GroupConfig]
  ): ZIO[MavenWireMock, Throwable, NonEmptyList[FoundMavenOrgSearchResult]] = {

    ZIO
      .foreach(orgConfigs) { orgConfig =>
        createStubbingsForOrgConfig(scalaVersion, orgConfig)
      }
      .map(_.flatten)
      .flatMap(a => ZIO.fromEither(a.asNonEmptyList))
  }

  private def createStubbingsForOrgConfig(
      scalaVersion: ValidScalaVersion,
      orgConfig: GroupConfig
  ): ZIO[MavenWireMock, Throwable, List[FoundMavenOrgSearchResult]] = {
    ZIO
      .foreach(orgConfig.modules) { moduleConfig =>
        val moduleName = moduleConfig.scalaVersionedName(scalaVersion)
        val rawSearchResult = RawSearchResult(orgConfig.org, moduleName, "1.0.0")
        val results = List(rawSearchResult)
        val searchResult = FoundMavenOrgSearchResult(rawSearchResult, moduleConfig)

        MavenWireMock.stubSearchModule(orgName = orgConfig.org, moduleName, results).as(List(searchResult))
      }
      .map(_.flatten)
  }

  private def stubWireMockForOrgConfig(
      scalaVersion: ValidScalaVersion,
      searchConfig: SearchConfig,
      orgConfigReferenceName: String
  ): ZIO[MavenWireMock, Throwable, NonEmptyList[FoundMavenOrgSearchResult]] = {
    for {
      orgConfigs <- ZIO.fromEither(searchConfig.getOrgConfigByReference(orgConfigReferenceName))
      searchResults <- createStubbingsForOrgConfigs(scalaVersion, List(orgConfigs))
    } yield searchResults
  }
}
