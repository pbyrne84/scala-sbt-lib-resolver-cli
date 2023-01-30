package com.pbyrne84.github.scala.github.mavensearchcli.config

import com.pbyrne84.github.scala.github.mavensearchcli.shared.BaseSpec
import com.pbyrne84.github.scala.github.mavensearchcli.shared.BaseSpec.SharedDeps
import zio.Scope
import zio.test._

import java.util.UUID

object ModuleConfigSpec extends BaseSpec {

  private val organisation = s"org1-${UUID.randomUUID()}"
  private val moduleName = s"module--${UUID.randomUUID()}"
  private val version = s"1.2-${UUID.randomUUID()}"

  override def spec: Spec[SharedDeps with TestEnvironment with Scope, Any] =
    suite("ModuleConfigSpec")(
      suite("render should")(
        test("render to a non test scala lib dependency") {
          val actual = ModuleConfig(name = moduleName, moduleType = ScalaNormalScope)
            .render(organisation = organisation, version = version)

          val expected =
            s"""
             |"$organisation" %% "$moduleName" % "$version"
             |""".stripMargin.trim

          assertTrue(actual == expected)
        },
        test("render to a test scala lib dependency") {
          val actual = ModuleConfig(name = moduleName, moduleType = ScalaTestScope)
            .render(organisation = organisation, version = version)

          val expected =
            s"""
             |"$organisation" %% "$moduleName" % "$version" % Test
             |""".stripMargin.trim

          assertTrue(actual == expected)
        },
        test("render to a non test java lib dependency") {
          val actual = ModuleConfig(name = moduleName, moduleType = JavaNormalScope)
            .render(organisation = organisation, version = version)

          val expected =
            s"""
             |"$organisation" % "$moduleName" % "$version"
             |""".stripMargin.trim

          assertTrue(actual == expected)
        },
        test("render to a test java lib dependency") {
          val actual = ModuleConfig(name = moduleName, moduleType = JavaTestScope)
            .render(organisation = organisation, version = version)

          val expected =
            s"""
             |"$organisation" % "$moduleName" % "$version" % Test
             |""".stripMargin.trim

          assertTrue(actual == expected)
        },
        test("render a sbt plugin") {
          val actual = ModuleConfig(name = moduleName, moduleType = SbtPlugin)
            .render(organisation = organisation, version = version)

          val expected =
            s"""
             |addSbtPlugin("$organisation" % "$moduleName" % "$version")
             |""".stripMargin.trim

          assertTrue(actual == expected)
        },
        test("render an sbt compiler plugin") {
          val actual = ModuleConfig(name = moduleName, moduleType = SbtCompilerPlugin)
            .render(organisation = organisation, version = version)

          val expected =
            s"""
             |addCompilerPlugin("$organisation" % "$moduleName" % "$version")
             |""".stripMargin.trim

          assertTrue(actual == expected)
        }
      )
    )
}
