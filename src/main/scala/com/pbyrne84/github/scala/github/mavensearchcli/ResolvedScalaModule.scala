package com.pbyrne84.github.scala.github.mavensearchcli

import scala.util.matching.Regex

object LatestVersion {

  private val versionApplyMethodMappings: Map[String, String => LatestVersion] = Map(
    "3" -> Latest3.apply,
    "2.12" -> Latest212.apply,
    "2.13" -> Latest213.apply
  )

  def fromFullName(fullName: String, moduleVersion: String): Either[Throwable, (String, LatestVersion)] = {
    val moduleNameWithVersionRegex: Regex = "([\\w-]+)_((\\d)((\\.\\d+)*))".r

    val (name, maybeScalaVersion) = fullName match {
      // how many _ are required is a fun game in itself
      case moduleNameWithVersionRegex(moduleName, scalaVersion, _, _, _) =>
        moduleName -> Some(scalaVersion)

      case _ => fullName -> None
    }

    maybeScalaVersion
      .map(scalaVersion => createResolvedScalaModule(name, scalaVersion, moduleVersion))
      .getOrElse(Right(name -> Java(moduleVersion)))
  }

  private def createResolvedScalaModule(
      name: String,
      scalaVersion: String,
      moduleVersion: String
  ): Either[RuntimeException, (String, LatestVersion)] = {
    versionApplyMethodMappings
      .get(scalaVersion)
      .map(apply => Right(name -> apply(moduleVersion)))
      .getOrElse(Left(new RuntimeException(s"Unknown scala version '$scalaVersion' for $name->$moduleVersion")))
  }
}

sealed abstract class LatestVersion(val version: String)
case class Latest212(override val version: String) extends LatestVersion(version)
case class Latest213(override val version: String) extends LatestVersion(version)
case class Latest3(override val version: String) extends LatestVersion(version)
case class Java(override val version: String) extends LatestVersion(version)

case class ResolvedScalaModule(org: String, name: String, latestVersions: List[LatestVersion]) {

  val maybeLatest212: Option[Latest212] =
    latestVersions.collectFirst { case version: Latest212 => version }

  val maybeLatest213: Option[Latest213] =
    latestVersions.collectFirst { case version: Latest213 => version }

  val maybeLatest3: Option[Latest3] =
    latestVersions.collectFirst { case version: Latest3 => version }

}
