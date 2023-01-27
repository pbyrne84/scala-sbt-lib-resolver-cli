package com.pbyrne84.github.scala.github.mavensearchcli.config

import io.circe.generic.extras.Configuration
import io.circe.{Decoder, DecodingFailure}

import scala.annotation.unused
import scala.util._

object ModuleConfig {
  import io.circe.generic.extras.semiauto._

  // used below but throws warning that it isn't, not scala3 compat as macros...
  @unused
  private implicit val customConfig: Configuration = Configuration.default.withDefaults

  implicit val moduleConfigDecoder: Decoder[ModuleConfig] = deriveConfiguredDecoder[ModuleConfig]
}
case class ModuleConfig(
    name: String,
    versionPattern: Option[String] = None,
    moduleType: ModuleType = ScalaNormalScope
) {

  def versionedName(scalaVersion: ScalaVersion): String = {
    if (moduleType.isScalaVersionedLib) {
      s"${name}_${scalaVersion.suffix}"
    } else {
      name
    }
  }

  def render(organisation: String, version: String): String =
    moduleType.render(organisation, name, version)

}
