package com.pbyrne84.github.scala.github.mavensearchcli.shared.wiremock

import com.pbyrne84.github.scala.github.mavensearchcli.config.CommandLineConfig
import com.pbyrne84.github.scala.github.mavensearchcli.maven.client.RawSearchResult
import com.pbyrne84.github.scala.github.mavensearchcli.shared.InitialisedPorts
import uk.org.devthings.scala.wiremockapi.remapping.RequestMethod.Get
import uk.org.devthings.scala.wiremockapi.remapping.WireMockExpectation
import zio.{Task, ZIO, ZLayer}

object MavenWireMock {

  private val zioService = ZIO.serviceWithZIO[MavenWireMock]

  val layer: ZLayer[CommandLineConfig with InitialisedPorts, Nothing, MavenWireMock] = ZLayer {
    for {
      initialisedPorts <- ZIO.service[InitialisedPorts]
      config <- ZIO.service[CommandLineConfig]

    } yield new MavenWireMock(new TestWireMock(initialisedPorts.mavenPort), config.mavenPageSize)

  }

  def reset: ZIO[MavenWireMock, Throwable, Unit] =
    zioService(_.reset)

  /** By default wiremock returns 404 when something is not stubbed which leads to not stubbing error cases. This leads
    * to confusing log output as it can indicate a problem in a wrong place, this detects there were a 404/near matches
    * and fails clearly enforcing a stub for failures.
    *
    * Also worth noting that there should be an instance of wiremock per thing mocked else near misses become absolutely
    * useless and tests become very sucky on failure.
    * @return
    */
  def verifyNoUnexpectedInteractions: ZIO[MavenWireMock, Throwable, true] =
    zioService(_.verifyNoUnexpectedInteractions)

  def stubSearchModule(
      orgName: String,
      moduleName: String,
      maybeStartTimestampSeconds: Option[Long],
      result: List[RawSearchResult]
  ): ZIO[MavenWireMock, Throwable, Unit] = {
    val query = maybeStartTimestampSeconds
      .map(startTimeStampSeconds => generateTimeLimitedOrgQuery(orgName, moduleName, startTimeStampSeconds * 1000))
      .getOrElse(generateOrgOnlyQuery(orgName, moduleName))

    stubSearchOrg(query, result)
  }

  private def generateTimeLimitedOrgQuery(orgName: String, moduleName: String, startTimeStampInMillis: Long): String = {
    s"g:$orgName AND a:$moduleName AND timestamp:[$startTimeStampInMillis TO *]"
  }

  private def generateOrgOnlyQuery(orgName: String, moduleName: String): String = {
    s"g:$orgName AND a:$moduleName"
  }

  private def stubSearchOrg(
      expectedQuery: String,
      result: List[RawSearchResult]
  ): ZIO[MavenWireMock, Throwable, Unit] =
    zioService(_.stubSearchOrg(expectedQuery, result))

  def stubInvalidResponseFormat(
      orgName: String,
      moduleName: String
  ): ZIO[MavenWireMock, Throwable, Unit] = {
    val expectedQuery = generateOrgOnlyQuery(orgName, moduleName)

    zioService(_.stubInvalidResponseFormat(expectedQuery))
  }

  def stubServerErrorResponse(
      orgName: String,
      moduleName: String
  ): ZIO[MavenWireMock, Throwable, Unit] = {
    val expectedQuery = generateOrgOnlyQuery(orgName, moduleName)

    zioService(_.stubServerErrorResponse(expectedQuery))
  }

}

class MavenWireMock(testWireMock: TestWireMock, pageSize: Int) {

  import WireMockExpectation.ops._

  def reset: Task[Unit] =
    testWireMock.reset

  def verifyNoUnexpectedInteractions: Task[true] =
    testWireMock.verifyNoUnexpectedInteractions

  def stubSearchOrg(
      expectedQuery: String,
      result: List[RawSearchResult]
  ): Task[Unit] = {
    if (result.isEmpty) {
      ZIO.attempt {
        stubSingleEntry(expectedQuery, result, List.empty, 1, 0)
      }
    } else {
      val paginatedResults: List[List[RawSearchResult]] = result.grouped(pageSize).toList
      ZIO.attempt {
        paginatedResults.zipWithIndex.map { case (paginatedResult, index) =>
          val currentPage = index + 1
          val start = (currentPage - 1) * pageSize
          stubSingleEntry(expectedQuery, result, paginatedResult, currentPage, start)
        }
      }
    }

  }

  private def stubSingleEntry(
      expectedQuery: String,
      result: List[RawSearchResult],
      paginatedResult: List[RawSearchResult],
      currentPage: Int,
      start: Int
  ): Unit = {

    import uk.org.devthings.scala.wiremockapi.remapping.WireMockExpectation.ops._

    testWireMock.addExpectation(
      WireMockExpectation.willRespondOk
        .expectsUrl("/select".asUrlPathEquals)
        .expectsMethod(Get)
        .expectsQueryParams(
          ("core" -> "gav").asEqualTo,
          ("start" -> start.toString).asEqualTo,
          ("rows" -> pageSize.toString).asEqualTo,
          ("q" -> expectedQuery).asEqualTo
        )
        .willRespondWithBody(createFullResponse(paginatedResult, result.size, currentPage).asJsonResponse)
    )
  }

  private def createFullResponse(result: List[RawSearchResult], numFound: Int, currentOffset: Int) = {
    import io.circe.syntax._

    // Tells intellij to highlight as json, maaaaaaaaagic
    // language=JSON
    s"""
      |{
      |  "responseHeader": {
      |    "status": 0,
      |    "QTime": 3,
      |    "params": {
      |      "q": "g:io.circe AND a:circe-yaml_2.13",
      |      "core": "gav",
      |      "indent": "off",
      |      "fl": "id,g,a,v,p,ec,timestamp,tags",
      |      "start": "0",
      |      "sort": "score desc,timestamp desc,g asc,a asc,v desc",
      |      "rows": "9",
      |      "wt": "json",
      |      "version": "2.2"
      |    }
      |  },
      |  "response": {
      |    "numFound": $numFound,
      |    "start": $currentOffset,
      |    "docs": ${result.asJson.spaces2}
      |  }
      |}
      |""".stripMargin
  }

  private def stubInvalidResponseFormat(expectedQuery: String): Task[Unit] = {
    ZIO.attempt {
      testWireMock.addExpectation(
        WireMockExpectation.willRespondOk
          .expectsUrl("/select".asUrlPathEquals)
          .expectsQueryParam(("q" -> expectedQuery).asEqualTo)
          .willRespondWithBody("{}".asJsonResponse)
      )
    }
  }

  private def stubServerErrorResponse(expectedQuery: String): Task[Unit] = {
    ZIO.attempt {
      testWireMock.addExpectation(
        WireMockExpectation.willRespondInternalServerError
          .expectsUrl("/select".asUrlPathEquals)
          .expectsQueryParam(("q" -> expectedQuery).asEqualTo)
          .willRespondWithBody("Fake internal server overload".asStringResponse)
      )
    }
  }
}
