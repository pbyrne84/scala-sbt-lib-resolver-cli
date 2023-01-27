package com.pbyrne84.github.scala.github.mavensearchcli.shared

import io.circe.ParsingFailure

class JsonOps {
  def formattedJson(json: String): Either[ParsingFailure, String] = {
    io.circe.parser.parse(json).map(_.spaces2)
  }

  // stops having to deal with unclear error later on and makes sure things are readable
  def formattedJsonUnsafe(json: String): String = {
    io.circe.parser.parse(json).map(_.spaces2) match {
      case Left(value) => throw new RuntimeException(s"cannot parse $json", value)
      case Right(value) => value
    }
  }
}
