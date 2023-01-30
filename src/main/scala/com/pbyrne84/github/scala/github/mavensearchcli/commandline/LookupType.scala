package com.pbyrne84.github.scala.github.mavensearchcli.commandline

import com.pbyrne84.github.scala.github.mavensearchcli.config.{GroupConfig, MissingHotListException, SearchConfig}

sealed trait LookupType {
  def getOrgConfigsForLookup(
      searchConfig: SearchConfig
  ): Either[MissingHotListException, List[GroupConfig]] =
    this match {
      case HotListLookupType(hotListName) =>
        searchConfig.getHotListOrgConfigs(hotListName)

      case ModuleGroupLookupType(moduleGroupName) =>
        searchConfig
          .getOrgConfigByReference(moduleGroupName)
          .map(foundOrgConfig => List(foundOrgConfig))
    }
}

case class HotListLookupType(hotListName: String) extends LookupType

case class ModuleGroupLookupType(moduleGroupName: String) extends LookupType
