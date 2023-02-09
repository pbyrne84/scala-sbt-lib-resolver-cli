package com.pbyrne84.github.scala.github.mavensearchcli.config

import io.circe.Decoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder

import scala.annotation.unused

object ConfigDefaults {
  // is used by extras.semiauto but compiler says otherwise then fails when removed
  // for scala 3 we can https://github.com/circe/circe/pull/1800
  @unused
  private implicit val customConfig: Configuration = Configuration.default.withDefaults.copy(transformMemberNames = {
    case "maybeHotList" =>
      "hotList"

    case other =>
      other
  })

  implicit val defaultConfigsDecoder: Decoder[ConfigDefaults] = deriveConfiguredDecoder[ConfigDefaults]

}
case class ConfigDefaults(productionVersionRegex: String = "([\\d\\.]+)", maybeHotList: Option[String] = None)
