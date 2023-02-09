package com.pbyrne84.github.scala.github.mavensearchcli.commandline

import com.pbyrne84.github.scala.github.mavensearchcli.config.{GroupConfig, MissingHotListException, SearchConfig}

sealed trait LookupType {
  def getOrgConfigsForLookup(
      searchConfig: SearchConfig
  ): Either[MissingHotListException, List[GroupConfig]] =
    this match {
      case DefaultHotListLookupType =>
        searchConfig.defaults.maybeHotList match {
          case Some(defaultHotListName) => searchConfig.getHotListOrgConfigs(hotListName = defaultHotListName)
          case None => Left(MissingHotListException("There is no default hotList configured in defaults.hotList"))
        }

      case CustomHotListLookupType(hotListName) =>
        searchConfig.getHotListOrgConfigs(hotListName)

      case ModuleGroupLookupType(moduleGroupName) =>
        searchConfig
          .getOrgConfigByReference(moduleGroupName)
          .map(foundOrgConfig => List(foundOrgConfig))
    }
}

sealed trait HotListLookupType extends LookupType
case object DefaultHotListLookupType extends HotListLookupType
case class CustomHotListLookupType(hotListName: String) extends HotListLookupType

case class ModuleGroupLookupType(moduleGroupName: String) extends LookupType
