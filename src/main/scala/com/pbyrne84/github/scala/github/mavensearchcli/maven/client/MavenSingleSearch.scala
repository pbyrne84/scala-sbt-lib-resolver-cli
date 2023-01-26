package com.pbyrne84.github.scala.github.mavensearchcli.maven.client

import com.pbyrne84.github.scala.github.mavensearchcli.ZIOServiced
import com.pbyrne84.github.scala.github.mavensearchcli.config.CommandLineConfig
import com.pbyrne84.github.scala.github.mavensearchcli.maven.MavenOrgSearchResults
import sttp.client3.Response
import sttp.client3.httpclient.zio.HttpClientZioBackend
import zio.config.ReadError
import zio.{Cause, IO, ZIO, ZLayer}

sealed abstract class SingleSearchException(message: String, cause: Throwable) extends RuntimeException(message, cause)

case class NetworkSingleSearchException(invalidUrl: String, cause: Throwable)
    extends SingleSearchException(s"The url '$invalidUrl' failed with ${cause.getMessage}", cause)

case class JsonDecodingSingleSearchException(cause: Throwable) extends SingleSearchException(cause.getMessage, cause)

case class UnexpectedSingleSearchException(cause: Throwable) extends SingleSearchException(cause.getMessage, cause)

object MavenSingleSearch extends ZIOServiced[MavenSingleSearch] {

  val layer: ZLayer[Any, ReadError[String], MavenSingleSearch] = ZLayer {
    for {
      config <- ZIO.service[CommandLineConfig].provide(CommandLineConfig.layer)
    } yield new MavenSingleSearch(config.mavenHost, config.mavenPageSize)
  }

  def runQuery(
      searchParams: SearchParams,
      startIndex: Int,
      maybeWithinSeconds: Option[Int]
  ): ZIO[NowProvider with MavenSingleSearch, SingleSearchException, MavenOrgSearchResults] =
    serviced(_.runQuery(searchParams, startIndex, maybeWithinSeconds))
}

class MavenSingleSearch(mavenHostUrl: String, pageSize: Int) {

  def runQuery(
      searchParams: SearchParams,
      startIndex: Int,
      maybeWithinSeconds: Option[Int]
  ): ZIO[NowProvider, SingleSearchException, MavenOrgSearchResults] = {
    import sttp.client3._ // all the basicRequest/asStringAlways stuff etc.

    val eventualSearchTerms: ZIO[NowProvider, Nothing, String] = maybeWithinSeconds
      .map { withinSeconds =>
        for {
          now <- NowProvider.getNow
          startTimeStamp = now.toEpochMilli - (withinSeconds.toLong * 1000)
        } yield s"g:${searchParams.orgName} AND a:${searchParams.versionedModuleName} AND timestamp:[$startTimeStamp TO *]"
      }
      .getOrElse(ZIO.succeed(s"g:${searchParams.orgName} AND a:${searchParams.versionedModuleName}"))

    (for {
      backend <- HttpClientZioBackend()
      searchTerms <- eventualSearchTerms
      searchUri =
        uri"${mavenHostUrl.stripSuffix("/")}/select?core=gav&q=$searchTerms&rows=$pageSize&start=$startIndex"
      _ <- ZIO.logDebug(s"calling maven with $searchUri from $searchParams")
      request = basicRequest
        .response(asStringAlways)
        .get(searchUri)
      response <- backend.send(request).mapError(error => NetworkSingleSearchException(searchUri.toString, error))
      _ <- ZIO.logDebug(s"received $response from $searchUri ")
      _ <- validateResponseStatus(response).mapError(error => NetworkSingleSearchException(searchUri.toString, error))
      searchResults <- ZIO
        .fromEither(MavenOrgSearchResults.fromJsonText(response.body))
        .mapError(error => JsonDecodingSingleSearchException(error))
    } yield searchResults).mapError(error => UnexpectedSingleSearchException(error))
  }

  private def validateResponseStatus(response: Response[String]): IO[UnExpectedMavenResponseError, Boolean] = {
    if (!response.code.isSuccess) {
      val error = UnExpectedMavenResponseError(
        s"${response.request} returned ${response.code} with body: ${response.body}"
      )

      ZIO.logErrorCause(Cause.fail(error)) *> ZIO.fail(error)
    } else {
      ZIO.logDebug("request was successful") *> ZIO.succeed(true)
    }
  }

}
