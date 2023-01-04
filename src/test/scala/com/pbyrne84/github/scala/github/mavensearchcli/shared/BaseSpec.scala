package com.pbyrne84.github.scala.github.mavensearchcli.shared

import com.pbyrne84.github.scala.github.mavensearchcli.config.CommandLineConfig
import com.pbyrne84.github.scala.github.mavensearchcli.shared.BaseSpec.SharedDeps
import com.pbyrne84.github.scala.github.mavensearchcli.shared.wiremock.MavenWireMock
import io.circe.ParsingFailure
import zio.{ZIO, ZLayer}
import zio.test.ZIOSpec

object BaseSpec {

  type SharedDeps = InitialisedPorts with MavenWireMock

  val sharedLayer = ZLayer.make[SharedDeps](
    InitialisedPorts.layer,
    MavenWireMock.layer,
    CommandLineConfig.layer
  )

}

abstract class BaseSpec extends ZIOSpec[SharedDeps] {
  override def bootstrap = BaseSpec.sharedLayer

  protected  def reset: ZIO[ MavenWireMock, Throwable, Unit ] = {
    MavenWireMock.reset
  }

  def formattedJson(json: String): Either[ParsingFailure, String] = {
    io.circe.parser.parse(json).map(_.spaces2)
  }

}
