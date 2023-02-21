package com.pbyrne84.github.scala.github.mavensearchcli.maven

import com.pbyrne84.github.scala.github.mavensearchcli.config._
import com.pbyrne84.github.scala.github.mavensearchcli.maven.client.RawSearchResult

object MavenOrgSearchResult {

  def comparable(mavenOrgSearchResult: MavenOrgSearchResult): String = {
    mavenOrgSearchResult match {
      case missing: MissingMavenOrgSearchResult =>
        s"1_${missing.organisation}_${missing.moduleName}"

      case found: FoundMavenOrgSearchResult =>
        // enables grouping when ordering at the end
        val typeMarker = found.moduleConfig.moduleType match {
          case ScalaNormalScope | JavaNormalScope => 1
          case ScalaTestScope | JavaTestScope => 2
          case SbtPlugin => 3
          case SbtCompilerPlugin => 4
        }

        s"0_${typeMarker}_${found.organisation}_${found.moduleConfig.name}"

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
