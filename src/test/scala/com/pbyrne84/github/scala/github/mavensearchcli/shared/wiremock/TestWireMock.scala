package com.pbyrne84.github.scala.github.mavensearchcli.shared.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import zio.{Task, ZIO}

object TestWireMock {}

class TestWireMock(val port: Int) {

  println(s"starting testwiremock on port $port")

  lazy val wireMock = new WireMockServer(port)

  def reset: Task[Unit] = {
    ZIO.attemptBlocking {
      if (!wireMock.isRunning) {
        wireMock.start()
      }

      wireMock.resetAll()
    }
  }

}
