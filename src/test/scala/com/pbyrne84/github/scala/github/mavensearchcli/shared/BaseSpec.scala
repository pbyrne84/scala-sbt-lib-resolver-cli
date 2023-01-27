package com.pbyrne84.github.scala.github.mavensearchcli.shared

import com.pbyrne84.github.scala.github.mavensearchcli.config.CommandLineConfig
import com.pbyrne84.github.scala.github.mavensearchcli.shared.BaseSpec.SharedDeps
import com.pbyrne84.github.scala.github.mavensearchcli.shared.wiremock.MavenWireMock
import zio.test.ZIOSpec
import zio.{ZIO, ZLayer}

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

  protected def reset: ZIO[MavenWireMock, Throwable, Unit] = {
    MavenWireMock.reset
  }

  // I have a common but bad habit of shoving everything in the base spec of tests.
  // The problem with this is when you want to auto complete stuff it is very hard to resolve
  // as there can be 200 methods starting with get or something. Dividing things by some sort of instance
  // helps alleviate this.
  protected val jsonOps = new JsonOps

}
