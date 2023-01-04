package com.pbyrne84.github.scala.github.mavensearchcli.maven

import com.pbyrne84.github.scala.github.mavensearchcli.maven.client.{RawSearchResult, UnExpectedMavenResponseError}
import io.circe.Json

object MavenOrgSearchResults {

  def fromJsonText(jsonText: String): Either[Throwable, MavenOrgSearchResults] = {
    import io.circe.parser.parse
    for {
      parsedJson <- parse(jsonText).left
        .map(error => UnExpectedMavenResponseError(s"Cannot parse $jsonText", Some(error)))

      totalRows <- convertTotalRows(parsedJson)
      convertedResponse <- convertResponse(parsedJson)
    } yield MavenOrgSearchResults(totalRows, convertedResponse)
  }

  private def convertTotalRows(parsedJson: Json): Either[UnExpectedMavenResponseError, Int] = {
    parsedJson.hcursor
      .downField("response")
      .downField("numFound")
      .as[Int]
      .left
      .map(error =>
        UnExpectedMavenResponseError(s"Could not find response.numFound in ${parsedJson.spaces2}", Some(error))
      )
  }

  private def convertResponse(parsedJson: Json): Either[Exception, List[RawSearchResult]] = {
    parsedJson.hcursor
      .downField("response")
      .downField("docs")
      .as[List[RawSearchResult]]
  }
}

case class MavenOrgSearchResults(totalFound: Int, pagedResults: List[RawSearchResult])
