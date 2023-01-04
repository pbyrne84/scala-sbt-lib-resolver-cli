package com.pbyrne84.github.scala.github.mavensearchcli.maven

import com.pbyrne84.github.scala.github.mavensearchcli.config.ModuleConfig
import com.pbyrne84.github.scala.github.mavensearchcli.maven.client.RawSearchResult

object MavenOrgSearchResult {

  def comparable(mavenOrgSearchResult: MavenOrgSearchResult): String = {
    mavenOrgSearchResult match {
      case found: FoundMavenOrgSearchResult =>
        def boolToInt(boolean: Boolean) = if (boolean) {
          1
        } else {
          0
        }

        // only really care about plugin order on success as they need a different implementation action
        val pluginMarker = boolToInt(found.isSbtPlugin)
        val testMarker = boolToInt(found.isTestScope)

        s"0_${pluginMarker}_${testMarker}_${found.organisation}_${found.moduleConfig.name}"

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
  val isSbtPlugin: Boolean = moduleConfig.isSbtPlugin
  val isTestScope: Boolean = moduleConfig.isTestScope

  override def render: String = {
    if (moduleConfig.isScala) {
      if (moduleConfig.isTestScope) {
        s"""
           |"$organisation" %% "${moduleConfig.name}" % "$version" % Test
           |""".stripMargin
      } else if (moduleConfig.isSbtPlugin) {
        s"""
          |addSbtPlugin("$organisation" % "${moduleConfig.name}" % "$version")
          |""".stripMargin
      } else {
        s"""
           |"$organisation" %% "${moduleConfig.name}" % "$version"
           |""".stripMargin
      }
    } else {
      if (moduleConfig.isTestScope) {
        s"""
           |"$organisation" % "${moduleConfig.name}" % "$version" % Test
           |""".stripMargin
      } else {
        s"""
           |"$organisation" % "${moduleConfig.name}" % "$version"
           |""".stripMargin
      }
    }
  }.trim
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
