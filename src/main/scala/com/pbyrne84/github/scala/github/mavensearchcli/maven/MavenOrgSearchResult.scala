package com.pbyrne84.github.scala.github.mavensearchcli.maven

import com.pbyrne84.github.scala.github.mavensearchcli.config.{
  JavaNormalScope,
  JavaTestScope,
  ModuleConfig,
  SbtCompilerPlugin,
  SbtPlugin,
  ScalaNormalScope,
  ScalaTestScope
}
import com.pbyrne84.github.scala.github.mavensearchcli.maven.client.RawSearchResult

object MavenOrgSearchResult {

  def comparable(mavenOrgSearchResult: MavenOrgSearchResult): String = {
    mavenOrgSearchResult match {
      case found: FoundMavenOrgSearchResult =>
        // only really care about plugin order on success as they need a different implementation action
        val typeMarker = found.moduleConfig.moduleType match {
          case ScalaNormalScope => 1
          case ScalaTestScope => 2
          case JavaNormalScope => 3
          case JavaTestScope => 4
          case SbtPlugin => 5
          case SbtCompilerPlugin => 6
        }

        s"0_${typeMarker}_${found.organisation}_${found.moduleConfig.name}"

      case missing: MissingMavenOrgSearchResult =>
        s"1_${missing.organisation}_${missing.moduleName}"

    }
  }

}

sealed trait MavenOrgSearchResult {
  def render: String
}

case class FoundMavenOrgSearchResult(rawSearchResult: RawSearchResult, moduleConfig: ModuleConfig)
    extends MavenOrgSearchResult {
  val moduleWithScalaVersion: String = rawSearchResult.moduleWithScalaVersion
  val version: String = rawSearchResult.version
  val organisation: String = rawSearchResult.organisation

  override def render: String = moduleConfig.render(organisation, version)
}

case class MissingMavenOrgSearchResult(
    organisation: String,
    moduleName: String,
    foundPotentialVersions: List[String]
) extends MavenOrgSearchResult {
  override def render: String = {
    if (foundPotentialVersions.isEmpty) {
      s"$organisation.$moduleName was not found at all"
    } else {
      s"$organisation.$moduleName was not found with a valid matching version. The following versions were found:" +
        foundPotentialVersions.mkString(", ")
    }

  }
}
