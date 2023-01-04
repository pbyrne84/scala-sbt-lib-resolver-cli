package com.pbyrne84.github.scala.github.mavensearchcli.shared.wiremock

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalTo}
import com.pbyrne84.github.scala.github.mavensearchcli.config.CommandLineConfig
import com.pbyrne84.github.scala.github.mavensearchcli.maven.client.RawSearchResult
import com.pbyrne84.github.scala.github.mavensearchcli.shared.InitialisedPorts
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

  def stubSearchOrg(expectedQuery: String, result: List[RawSearchResult]): ZIO[MavenWireMock, Throwable, Unit] =
    zioService(_.stubSearchOrg(expectedQuery, result))

}

class MavenWireMock(testWireMock: TestWireMock, pageSize: Int) {

//GET https://search.maven.org/solrsearch/select?q=g:io.circe%20AND%20a:circe-yaml_2.13&core=gav&start=0&rows=9
// https://search.maven.org/solrsearch/select?q=g:io.circe&rows=100&wt=json

  def reset: Task[Unit] =
    testWireMock.reset

  def stubSearchOrg(expectedQuery: String, result: List[RawSearchResult]): Task[Unit] = {
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
    testWireMock.wireMock.stubFor(
      WireMock
        .get(WireMock.urlPathMatching("/select"))
        .withQueryParam("core", equalTo("gav"))
        .withQueryParam("start", equalTo(start.toString))
        .withQueryParam("rows", equalTo(pageSize.toString))
        .withQueryParam("q", equalTo(s"$expectedQuery"))
        .willReturn(aResponse().withBody(createFullResponse(paginatedResult, result.size, currentPage)))
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

}
