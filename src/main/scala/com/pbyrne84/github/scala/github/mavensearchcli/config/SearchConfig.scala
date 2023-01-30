package com.pbyrne84.github.scala.github.mavensearchcli.config
import io.circe.Decoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._

import scala.annotation.{tailrec, unused}

case class MissingHotListException(message: String) extends RuntimeException(message)

object SearchConfig {

  // is used by extras.semiauto but compiler says otherwise then fails when removed
  @unused
  private implicit val customConfig: Configuration = Configuration.default.withDefaults

  implicit val commandLineConfigDecoder: Decoder[SearchConfig] = deriveConfiguredDecoder[SearchConfig]

  implicit val orgConfigDecoder: Decoder[GroupConfig] = deriveConfiguredDecoder[GroupConfig]

  def decodeFromString(input: String): Either[RuntimeException, SearchConfig] =
    io.circe.parser
      .decode[SearchConfig](input)
      .left
      .map(error => new RuntimeException(s"${error.getMessage} cannot parse $input", error))

}

case class SearchConfig(
    defaultProductionVersionRegex: String,
    maximumPagesToPaginate: Int, // search.maven.org can ban you for a while if too greedy
    private[config] val hotLists: List[HotListItemConfig],
    groups: List[GroupConfig],
    retryCount: Int = 0
) {
  private val allLibNames = groups.map(_.name)
  val isValid: Boolean = hotLists.flatMap(_.refs).exists(ref => allLibNames.contains(ref))

  val errorOrHotListMappings: Either[MissingHotListException, Map[String, List[GroupConfig]]] =
    processHotList(hotLists, Map.empty)

  def getHotListOrgConfigs(hotListName: String): Either[MissingHotListException, List[GroupConfig]] = {
    for {
      hotListMappings <- errorOrHotListMappings
      foundOrgConfigs <- hotListMappings
        .get(hotListName)
        .toRight(
          MissingHotListException(s"Could not find hotList '$hotLists' in ${hotListMappings.keys.mkString(", ")}")
        )
    } yield foundOrgConfigs
  }

  def getOrgConfigByReference(referenceName: String): Either[MissingHotListException, GroupConfig] = {
    groups
      .find(_.name == referenceName)
      .toRight(
        MissingHotListException(
          s"Could not find a lib with the reference $referenceName, available references ${groups.map(_.name).mkString(", ")}"
        )
      )

  }

  @tailrec
  private def processHotList(
      remainingHotListItems: List[HotListItemConfig],
      hotListMappings: Map[String, List[GroupConfig]]
  ): Either[MissingHotListException, Map[String, List[GroupConfig]]] = {
    remainingHotListItems match {
      case ::(currentHotListItemConfig: HotListItemConfig, next: List[HotListItemConfig]) =>
        val maybeResolved: List[(String, Option[GroupConfig])] = currentHotListItemConfig.refs.map { ref: String =>
          ref -> groups.find(_.name == ref)
        }

        val missing = maybeResolved.collect { case (name, None) => name }
        val found = maybeResolved.collect { case (_, Some(orgConfig)) => orgConfig }

        if (missing.nonEmpty) {
          Left(MissingHotListException(s"The following lib configs are missing '${missing.mkString(", ")}'"))
        } else {
          val updatedMappings: Map[String, List[GroupConfig]] =
            hotListMappings + (currentHotListItemConfig.name -> found)
          processHotList(next, updatedMappings)
        }

      case Nil =>
        Right(hotListMappings)
    }

  }
}

case class GroupConfig(name: String, org: String, modules: List[ModuleConfig])

object HotListItemConfig {
  implicit val hotListItemConfigDecoder: Decoder[HotListItemConfig] =
    io.circe.generic.semiauto.deriveDecoder[HotListItemConfig]
}
case class HotListItemConfig(name: String, refs: List[String])
