package com.pbyrne84.github.scala.github.mavensearchcli.maven.client

import com.pbyrne84.github.scala.github.mavensearchcli.ZIOServiced
import zio.{ULayer, ZIO, ZLayer}

import java.time.Instant

object NowProvider extends ZIOServiced[NowProvider] {

  val layer: ULayer[NowProvider] =
    ZLayer.succeed(new NowProvider())

  def getNow: ZIO[NowProvider, Nothing, Instant] = serviced(_.getNow)
}

class NowProvider {

  def getNow: ZIO[Any, Nothing, Instant] = {
    ZIO.succeed(Instant.now())
  }
}
