package com.pbyrne84.github.scala.github.mavensearchcli.config
import io.circe
import io.circe.Decoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._

import scala.annotation.tailrec

object SearchConfig {

  // is used by extras.semiauto
  private implicit val customConfig: Configuration = Configuration.default.withDefaults

  implicit val commandLineConfigDecoder: Decoder[SearchConfig] = deriveConfiguredDecoder[SearchConfig]

  implicit val orgConfigDecoder: Decoder[OrgConfig] = deriveConfiguredDecoder[OrgConfig]

  def decodeFromString(input: String): Either[RuntimeException, SearchConfig] =
    io.circe.parser
      .decode[SearchConfig](input)
      .left
      .map(error => new RuntimeException(s"${error.getMessage} cannot parse $input", error))

}

case class SearchConfig(
    defaultProductionVersionRegex: String,
    maximumPagesToPaginate: Int, // search.maven.org can ban you for a while if too greedy
    private[config] val hotList: List[HotListItemConfig],
    libs: List[OrgConfig],
    retryCount: Int = 0
) {
  private val allLibNames = libs.map(_.name)
  val isValid: Boolean = hotList.flatMap(_.refs).exists(ref => allLibNames.contains(ref))

  val errorOrHotListMappings: Either[String, Map[String, List[OrgConfig]]] = processHotList(hotList, Map.empty)

  @tailrec
  private def processHotList(
      remainingHotListItems: List[HotListItemConfig],
      hotListMappings: Map[String, List[OrgConfig]]
  ): Either[String, Map[String, List[OrgConfig]]] = {
    remainingHotListItems match {
      case ::(currentHotListItemConfig: HotListItemConfig, next: List[HotListItemConfig]) =>
        val maybeResolved: List[(String, Option[OrgConfig])] = currentHotListItemConfig.refs.map { ref: String =>
          ref -> libs.find(_.name == ref)
        }

        val missing = maybeResolved.collect { case (name, None) => name }
        val found = maybeResolved.collect { case (_, Some(orgConfig)) => orgConfig }

        if (missing.nonEmpty) {
          Left(s"The following lib configs are missing '${missing.mkString(", ")}'")
        } else {
          val updatedMappings: Map[String, List[OrgConfig]] = hotListMappings + (currentHotListItemConfig.name -> found)
          processHotList(next, updatedMappings)
        }

      case Nil =>
        Right(hotListMappings)
    }

  }
}

object HotListItemConfig {
  implicit val hotListItemConfigDecoder: Decoder[HotListItemConfig] =
    io.circe.generic.semiauto.deriveDecoder[HotListItemConfig]
}
case class HotListItemConfig(name: String, refs: List[String])

case class OrgConfig(name: String, org: String, modules: List[ModuleConfig])

object ModuleConfig {
  import io.circe.generic.extras.semiauto._

  // used below, not scala3 compat as macros...
  private implicit val customConfig: Configuration = Configuration.default.withDefaults

  implicit val moduleConfigDecoder: Decoder[ModuleConfig] = deriveConfiguredDecoder[ModuleConfig]
}
case class ModuleConfig(
    name: String,
    isTestScope: Boolean = false,
    versionPattern: Option[String] = None,
    isScala: Boolean = true,
    isSbtPlugin: Boolean = false
) {
  def versionedName(scalaVersion: ScalaVersion): String = if (isScala && !isSbtPlugin) {
    s"${name}_${scalaVersion.suffix}"
  } else {
    name
  }
}
