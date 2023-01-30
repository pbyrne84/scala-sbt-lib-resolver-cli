package com.pbyrne84.github.scala.github.mavensearchcli.config

import io.circe.{Decoder, DecodingFailure}

import scala.util.{Failure, Success}

object ModuleType {

  private val valueMap: Map[String, ModuleType] = List(
    ScalaNormalScope,
    ScalaTestScope,
    JavaNormalScope,
    JavaTestScope,
    SbtPlugin,
    SbtCompilerPlugin
  ).map(moduleType => moduleType.textValue -> moduleType).toMap

  private val allStringKeys: List[String] = valueMap.keys.toList

  // Usually I would use enumeratum for this
  implicit val moduleTypeDecoder: Decoder[ModuleType] = {
    Decoder.decodeString.emapTry { (stringValue: String) =>
      valueMap
        .get(stringValue)
        .map(Success.apply)
        .getOrElse(
          Failure(DecodingFailure(s"'$stringValue' could not resolve to ${allStringKeys.mkString(", ")}", List.empty))
        )
    }
  }

}

sealed abstract class ModuleType(val textValue: String) {
  def render(organisation: String, moduleName: String, version: String): String

  // unless it lazy/def then it is null
  lazy val isScalaVersionedLib: Boolean = this match {
    case _: ScalaNormalScope.type => true
    case _: ScalaTestScope.type => true
    case _: SbtCompilerPlugin.type => true
    case _ => false
  }
}
case object ScalaNormalScope extends ModuleType("scala") {
  def render(organisation: String, moduleName: String, version: String): String =
    s"""
       |"$organisation" %% "$moduleName" % "$version"
       |""".stripMargin.trim

}
case object ScalaTestScope extends ModuleType("scala-test") {
  def render(organisation: String, moduleName: String, version: String): String =
    s"""
       |"$organisation" %% "$moduleName" % "$version" % Test
       |""".stripMargin.trim

}
case object JavaNormalScope extends ModuleType("java") {
  def render(organisation: String, moduleName: String, version: String): String =
    s"""
       |"$organisation" % "$moduleName" % "$version"
       |""".stripMargin.trim
}

case object JavaTestScope extends ModuleType("java-test") {
  def render(organisation: String, moduleName: String, version: String): String =
    s"""
       |"$organisation" % "$moduleName" % "$version" % Test
       |""".stripMargin.trim
}
case object SbtPlugin extends ModuleType("plugin") {
  def render(organisation: String, moduleName: String, version: String): String =
    s"""
       |addSbtPlugin("$organisation" % "$moduleName" % "$version")
       |""".stripMargin.trim

}
case object SbtCompilerPlugin extends ModuleType("compiler-plugin") {
  override def render(organisation: String, moduleName: String, version: String): String =
    s"""
       |addCompilerPlugin("$organisation" % "$moduleName" % "$version")
       |""".stripMargin.trim

}
