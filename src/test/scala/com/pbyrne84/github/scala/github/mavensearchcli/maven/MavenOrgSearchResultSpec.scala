package com.pbyrne84.github.scala.github.mavensearchcli.maven

import com.pbyrne84.github.scala.github.mavensearchcli.MavenSearchCliApp.getClass
import com.pbyrne84.github.scala.github.mavensearchcli.config.ModuleConfig
import com.pbyrne84.github.scala.github.mavensearchcli.maven.client.RawSearchResult
import com.pbyrne84.github.scala.github.mavensearchcli.shared.BaseSpec
import com.pbyrne84.github.scala.github.mavensearchcli.shared.BaseSpec.SharedDeps
import zio.Scope
import zio.test._

import java.io.File

object MavenOrgSearchResultSpec extends BaseSpec {
  private implicit class StringOps(string: String) {
    def trimmedStripMargin: String = string.stripMargin.trim
  }

  override def spec: Spec[SharedDeps with TestEnvironment with Scope, Any] =
    suite("MavenOrgSearchResult")(
      suite("should render when the result")(
        test("is not found but versions were found but not matched") {
          val missingMavenOrgSearchResult =
            MissingMavenOrgSearchResult("orgSearchTerm1", "moduleSearchTerm1", List("v1", "v2", "v3"))

          val expected =
            "orgSearchTerm1.moduleSearchTerm1 was not found with a valid matching version. The following versions were found:" +
              "v1, v2, v3"

          assertTrue(
            missingMavenOrgSearchResult.render == expected
          )
        },
        test("is not found at all") {
          val missingMavenOrgSearchResult =
            MissingMavenOrgSearchResult("orgSearchTerm1", "moduleSearchTerm1", List.empty)

          val expected =
            "orgSearchTerm1.moduleSearchTerm1 was not found at all"

          assertTrue(
            missingMavenOrgSearchResult.render == expected
          )
        },
        test("is found and it is a scala module that is not a test or plugin") {
          val foundMavenOrgSearchResult =
            FoundMavenOrgSearchResult(
              rawSearchResult = RawSearchResult("dev.me", "me_212", "1.2"),
              moduleConfig = ModuleConfig("me")
            )

          val expected =
            """
              |"dev.me" %% "me" % "1.2"
              |""".trimmedStripMargin

          assertTrue(
            foundMavenOrgSearchResult.render == expected
          )
        },
        test("is found and it is a scala module that is a test") {
          val foundMavenOrgSearchResult =
            FoundMavenOrgSearchResult(
              rawSearchResult = RawSearchResult("dev.me", "me_212", "1.2"),
              moduleConfig = ModuleConfig("me", isTestScope = true)
            )

          val expected =
            """
              |"dev.me" %% "me" % "1.2" % Test
              |""".trimmedStripMargin

          assertTrue(
            foundMavenOrgSearchResult.render == expected
          )
        },
        test("is found and it is a scala sbt plugin") {
          val foundMavenOrgSearchResult =
            FoundMavenOrgSearchResult(
              rawSearchResult = RawSearchResult("dev.me", "me_212", "1.2"),
              moduleConfig = ModuleConfig("me", isSbtPlugin = true)
            )

          val expected =
            """
              |addSbtPlugin("dev.me" % "me" % "1.2")
              |""".trimmedStripMargin

          assertTrue(
            foundMavenOrgSearchResult.render == expected
          )
        },
        test("is found and it is a non test java dependency") {
          val foundMavenOrgSearchResult =
            FoundMavenOrgSearchResult(
              rawSearchResult = RawSearchResult("dev.me", "me_212", "1.2"),
              moduleConfig = ModuleConfig("me", isScala = false)
            )

          val expected =
            """
              |"dev.me" % "me" % "1.2"
              |""".trimmedStripMargin

          assertTrue(
            foundMavenOrgSearchResult.render == expected
          )
        },
        test("is found and it is a test java dependency") {
          val foundMavenOrgSearchResult =
            FoundMavenOrgSearchResult(
              rawSearchResult = RawSearchResult("dev.me", "me_212", "1.2"),
              moduleConfig = ModuleConfig("me", isScala = false, isTestScope = true)
            )

          val expected =
            """
              |"dev.me" % "me" % "1.2" % Test
              |""".trimmedStripMargin

          assertTrue(
            foundMavenOrgSearchResult.render == expected
          )
        }
      ),
      suite("ordering")(
        test(
          "should go non test, tests, plugins and then not found. Items under each grouping should be ordered by org and then module"
        ) {

          new File(getClass.getClassLoader.getResource("config.json").getFile.stripPrefix("\\").stripPrefix("/"))

          val org1NotFoundModule2 = createNotFound(organisation = "org1", moduleName = "module2")
          val org1FoundModule3 =
            createFound(organisation = "org1", moduleName = "module3", isTest = false, isSbtPlugin = false)

          val org1FoundTestModule0 =
            createFound(organisation = "org1", moduleName = "module0", isTest = true, isSbtPlugin = false)

          val org1FoundTestModule6 =
            createFound(organisation = "org1", moduleName = "module6", isTest = true, isSbtPlugin = false)

          val org1FoundPluginModule4 =
            createFound(organisation = "org1", moduleName = "module4", isTest = false, isSbtPlugin = true)

          val org1FoundPluginModule5 =
            createFound(organisation = "org1", moduleName = "module5", isTest = false, isSbtPlugin = true)

          val org2NotFoundModule1 = createNotFound(organisation = "org2", moduleName = "module1")
          val org2NotFoundModule2 = createNotFound(organisation = "org2", moduleName = "module2")
          val org2FoundModule3 =
            createFound(organisation = "org2", moduleName = "module3", isTest = false, isSbtPlugin = false)

          val org2FoundTestModule0 =
            createFound(organisation = "org2", moduleName = "module0", isTest = true, isSbtPlugin = false)

          val org2FoundPluginModule4 =
            createFound(organisation = "org2", moduleName = "module4", isTest = false, isSbtPlugin = true)
          val org2FoundModule5 =
            createFound(organisation = "org2", moduleName = "module5", isTest = false, isSbtPlugin = false)

          val results = List(
            org2FoundModule5,
            org1FoundPluginModule4,
            org1NotFoundModule2,
            org2NotFoundModule1,
            org1FoundModule3,
            org2NotFoundModule2,
            org1FoundPluginModule5,
            org2FoundModule3,
            org2FoundPluginModule4,
            org2FoundTestModule0,
            org1FoundTestModule6,
            org1FoundTestModule0
          )

          val expected = List(
            org1FoundModule3,
            org2FoundModule3,
            org2FoundModule5,
            org1FoundTestModule0,
            org1FoundTestModule6,
            org2FoundTestModule0,
            org1FoundPluginModule4,
            org1FoundPluginModule5,
            org2FoundPluginModule4,
            org1NotFoundModule2,
            org2NotFoundModule1,
            org2NotFoundModule2
          )

          val actual = results.sortBy(MavenOrgSearchResult.comparable)
          assertTrue(actual == expected)
        }
      )
    )

  private def createNotFound(organisation: String, moduleName: String) =
    MissingMavenOrgSearchResult(
      organisation = organisation,
      moduleName = moduleName,
      foundPotentialVersions = List.empty
    )

  private def createFound(organisation: String, moduleName: String, isTest: Boolean, isSbtPlugin: Boolean) =
    FoundMavenOrgSearchResult(
      rawSearchResult = RawSearchResult(
        organisation = organisation,
        moduleWithScalaVersion = "xxxx",
        version = ""
      ),
      moduleConfig = ModuleConfig(moduleName, isTestScope = isTest, isSbtPlugin = isSbtPlugin)
    )
}
