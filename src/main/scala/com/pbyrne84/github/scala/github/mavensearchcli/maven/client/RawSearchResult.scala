package com.pbyrne84.github.scala.github.mavensearchcli.maven.client

import io.circe.{Decoder, Encoder}

object RawSearchResult {
  implicit val rawModuleEncoder: Encoder.AsObject[RawSearchResult] =
    Encoder.forProduct3("g", "a", "v")(result =>
      (
        result.organisation,
        result.moduleWithScalaVersion,
        result.version
      )
    )

  implicit val rawModuleDecoder: Decoder[RawSearchResult] =
    Decoder.forProduct3("g", "a", "v")(RawSearchResult.apply)
}
case class RawSearchResult(organisation: String, moduleWithScalaVersion: String, version: String)
