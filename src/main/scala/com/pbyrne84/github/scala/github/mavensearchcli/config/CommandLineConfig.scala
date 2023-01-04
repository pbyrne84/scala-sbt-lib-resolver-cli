package com.pbyrne84.github.scala.github.mavensearchcli.config

import com.typesafe.config.{ConfigFactory, ConfigParseOptions}
import zio.config.typesafe.TypesafeConfig
import zio.{Layer, ZIO}

object CommandLineConfig {

  import zio.config._
  import ConfigDescriptor._

  val layer: Layer[ReadError[String], CommandLineConfig] =
    TypesafeConfig.fromTypesafeConfig(
      configTask,
      (string("mavenHost") zip int("mavenPageSize")).to[CommandLineConfig]
    )

  private lazy val configTask = {
    ZIO.attempt(ConfigFactory.load(ConfigParseOptions.defaults().setAllowMissing(true)))
  }
}

case class CommandLineConfig(mavenHost: String, mavenPageSize: Int)
