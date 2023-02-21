package com.pbyrne84.github.scala.github.mavensearchcli.config

import cats.data.{NonEmptyList, Validated}
import com.pbyrne84.github.scala.github.mavensearchcli.error.InvalidScalaVersionException
import io.circe.Decoder

import scala.util.{Failure, Success}

sealed trait ScalaVersion

case object DefaultScalaVersion extends ScalaVersion

object ValidScalaVersion {

  implicit val validScalaVersionDecoder: Decoder[ValidScalaVersion] =
    Decoder.decodeString.emapTry { value =>
      getVersionFromString(value) match {
        case Some(value) => Success(value)
        case None => Failure(InvalidScalaVersionException(value))
      }
    }

  private def getVersionFromString(value: String): Option[ValidScalaVersion] = {
    commandLineVersionMapping.get(value)
  }

  private val commandLineVersionMapping: Map[String, ValidScalaVersion] =
    List(
      ScalaVersion212,
      ScalaVersion213,
      ScalaVersion3
    ).map(version => version.versionText -> version).toMap

  private val validVersions: List[String] = commandLineVersionMapping.keys.toList

  def fromStringValue(value: String): Validated[NonEmptyList[String], ValidScalaVersion] =
    getVersionFromString(value) match {
      case Some(value) => Validated.valid(value)
      case None =>
        Validated.invalidNel(
          createInvalidScalaVersionMessage(value)
        )
    }

  def createInvalidScalaVersionMessage(value: String): String = {
    s"Scala version '$value' is unknown, possible values are ${validVersions.mkString(", ")}"
  }

}

sealed abstract class ValidScalaVersion(val versionText: String) extends ScalaVersion
case object ScalaVersion212 extends ValidScalaVersion("2.12")
case object ScalaVersion213 extends ValidScalaVersion("2.13")
case object ScalaVersion3 extends ValidScalaVersion("3")
