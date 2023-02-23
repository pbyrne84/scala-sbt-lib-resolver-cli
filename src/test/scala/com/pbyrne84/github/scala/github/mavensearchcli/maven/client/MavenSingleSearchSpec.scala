package com.pbyrne84.github.scala.github.mavensearchcli.maven.client
import com.pbyrne84.github.scala.github.mavensearchcli.config.{
  ModuleConfig,
  ScalaNormalScope,
  ScalaVersion212,
  ScalaVersion213
}
import com.pbyrne84.github.scala.github.mavensearchcli.error.{
  JsonDecodingSingleSearchException,
  NetworkSingleSearchException
}
import com.pbyrne84.github.scala.github.mavensearchcli.maven.MavenOrgSearchResults
import com.pbyrne84.github.scala.github.mavensearchcli.shared.BaseSpec
import com.pbyrne84.github.scala.github.mavensearchcli.shared.BaseSpec.SharedDeps
import com.pbyrne84.github.scala.github.mavensearchcli.shared.wiremock.MavenWireMock
import zio.test.Assertion._
import zio.test.TestAspect.sequential
import zio.test._
import zio.{Scope, ZIO, ZLayer}

import java.time.Instant

//Easier and more importantly clearer to test error remapping at this level
object MavenSingleSearchSpec extends BaseSpec {
  import org.mockito.Mockito._
  private val nowProvider: NowProvider = mock(classOf[NowProvider])
  private val nowProviderMockLayer = ZLayer.succeed(nowProvider)

  override def spec: Spec[SharedDeps with TestEnvironment with Scope, Any] =
    suite(getClass.getSimpleName)(
      suite("runQuery should")(
        test("return a search result when found and there is no time limit") {
          for {
            _ <- reset
            moduleConfig = ModuleConfig("module-name", Some("*.*"), ScalaNormalScope)
            params = SearchParams("org", moduleConfig, ScalaVersion213, "*.*", None, 3, 5)
            expected = RawSearchResult("found-org", "module-name_2.13", "212")
            _ <- MavenWireMock.stubSearchModule(params.orgName, expected.moduleWithScalaVersion, None, List(expected))
            result <- MavenSingleSearch.runQuery(params, 0)
            _ <- MavenWireMock.verifyNoUnexpectedInteractions
          } yield assertTrue(result == MavenOrgSearchResults(1, List(expected)))
        },
        test("return a search result within a time limit") {
          for {
            _ <- reset
            moduleConfig = ModuleConfig("module-name", Some("*.*"), ScalaNormalScope)
            params = SearchParams(
              "org",
              moduleConfig,
              ScalaVersion213,
              "*.*",
              maybeWithinSeconds = Some(1),
              maxPagesToPaginate = 3,
              retryCount = 5
            )
            _ = when(nowProvider.getNow)
              .thenReturn(ZIO.succeed(Instant.ofEpochSecond(10)))
            expected = RawSearchResult("found-org", "module-name_2.13", "212")
            _ <- MavenWireMock.stubSearchModule(
              params.orgName,
              expected.moduleWithScalaVersion,
              Some(9),
              List(expected)
            )
            result <- MavenSingleSearch.runQuery(params, 0)
            _ <- MavenWireMock.verifyNoUnexpectedInteractions
          } yield assertTrue(result == MavenOrgSearchResults(1, List(expected)))
        },
        test("return an error when there is a networking issue") {
          for {
            _ <- reset
            moduleConfig = ModuleConfig("module-name", Some("*.*"), ScalaNormalScope)
            params = SearchParams(
              "org",
              moduleConfig,
              ScalaVersion213,
              "*.*",
              maybeWithinSeconds = None,
              maxPagesToPaginate = 3,
              retryCount = 0
            )
            _ <- MavenWireMock.stubServerErrorResponse(
              params.orgName,
              moduleConfig.name + "_" + ScalaVersion213.versionText
            )
            _ = when(nowProvider.getNow)
              .thenReturn(ZIO.succeed(Instant.ofEpochSecond(10)))
            result <- MavenSingleSearch.runQuery(params, 0).exit
            _ <- MavenWireMock.verifyNoUnexpectedInteractions
          } yield assert(result)(fails(isSubtype[NetworkSingleSearchException](anything)))
        },
        test("return an error when there is a response that cannot be converted") {
          for {
            _ <- reset
            moduleConfig = ModuleConfig("module-name", Some("*.*"), ScalaNormalScope)
            params = SearchParams(
              "org",
              moduleConfig,
              ScalaVersion212,
              "*.*",
              maybeWithinSeconds = None,
              maxPagesToPaginate = 3,
              retryCount = 5
            )
            _ <- MavenWireMock.stubInvalidResponseFormat(
              params.orgName,
              moduleConfig.name + "_" + ScalaVersion212.versionText
            )
            _ = when(nowProvider.getNow)
              .thenReturn(ZIO.succeed(Instant.ofEpochSecond(10)))
            result <- MavenSingleSearch.runQuery(params, 0).exit
            _ <- MavenWireMock.verifyNoUnexpectedInteractions
          } yield assert(result)(fails(isSubtype[JsonDecodingSingleSearchException](anything)))
        }
      )
    ).provideSome[MavenWireMock](MavenSearchClient.layer, nowProviderMockLayer, MavenSingleSearch.layer) @@ sequential
}
