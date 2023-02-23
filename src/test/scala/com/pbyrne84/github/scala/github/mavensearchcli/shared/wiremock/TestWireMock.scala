package com.pbyrne84.github.scala.github.mavensearchcli.shared.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import zio.{Task, ZIO}
import scala.jdk.CollectionConverters.CollectionHasAsScala

object TestWireMock {}

class TestWireMock(val port: Int) {

  println(s"starting TestWireMock on port $port")

  lazy val wireMock = new WireMockServer(port)

  def reset: Task[Unit] = {
    ZIO.attempt {
      if (!wireMock.isRunning) {
        wireMock.start()
      }

      wireMock.resetAll()
    }
  }

  def verifyNoUnexpectedInteractions: Task[true] = {
    ZIO.fromEither {
      val nearMisses = wireMock.findNearMissesForUnmatchedRequests().getNearMisses.asScala.toList
      val unexpectedRequests = wireMock.findAllUnmatchedRequests().asScala.toList

      if (nearMisses.isEmpty && unexpectedRequests.isEmpty) {
        Right(true)
      } else {
        // Near misses rely on stubs to be set up.
        val nearMissErrorMessages = "\n** The following requests were near misses **\n" +
          nearMisses.map(request => request.toString).mkString("\n")

        val notMatchedErrorMessages = "\n** The following requests were not matched **\n" +
          unexpectedRequests.map(_.toString)

        Left(new RuntimeException(nearMissErrorMessages + notMatchedErrorMessages))
      }
    }
  }

}
