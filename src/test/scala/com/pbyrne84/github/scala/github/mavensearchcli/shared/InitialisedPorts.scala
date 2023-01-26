package com.pbyrne84.github.scala.github.mavensearchcli.shared

import zio.{ZIO, ZLayer}

import java.net.ServerSocket
import scala.util.Using

object InitialisedPorts {
  val layer: ZLayer[Any, Throwable, InitialisedPorts] = ZLayer {
    for {
      mavenPort <- initGetPort("MAVEN_PORT")
    } yield InitialisedPorts(mavenPort)

  }

  private def initGetPort(configName: String): ZIO[Any, Throwable, Int] = {
    ZIO.attempt {
      Option(System.getProperty(configName))
        .map { result => ZIO.attempt(result.toInt) }
        .getOrElse {
          ZIO.fromEither(calculateFreePort).map { port =>
            System.setProperty(configName, port.toString)
            port
          }
        }

    }.flatten
  }

  private def calculateFreePort: Either[Throwable, Int] = {
    Using(new ServerSocket(0)) { serverSocket: ServerSocket =>
      serverSocket.getLocalPort
    }.toEither
  }
}

case class InitialisedPorts(mavenPort: Int)
