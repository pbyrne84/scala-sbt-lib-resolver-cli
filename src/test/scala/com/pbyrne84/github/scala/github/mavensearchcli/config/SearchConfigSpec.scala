package com.pbyrne84.github.scala.github.mavensearchcli.config

import com.pbyrne84.github.scala.github.mavensearchcli.shared.BaseSpec
import io.circe.Json
import zio.Scope
import zio.test._

object SearchConfigSpec extends BaseSpec {
  // usually we do not want explicit default parameter values but it can make things clearer in tests, especially booleans
  // noinspection RedundantDefaultArgument
  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("SearchConfig")(
      test("should convert when there are hotList entries and validate entries exist in libs") {
        val json = {
          s"""
            |{
            |  "defaultProductionVersionRegex" : ".*",
            |  "maximumPagesToPaginate" : 2,
            |  "hotList" : [
            |     {
            |       "name" : "custom",
            |       "refs" : [ "circe", "zio" ]
            |     },{
            |       "name" : "hotCirce",
            |       "refs" : [ "circe"]
            |     }
            |  ],
            |  "libs": [
            |    {
            |      "name": "circe",
            |      "org": "io.circe",
            |      "modules": [
            |        ${moduleJson("circe-core").spaces2}
            |      ]
            |    },
            |    {
            |      "name": "zio",
            |      "org": "dev.zio",
            |      "modules": [
            |         ${moduleJson("zio-logging-slf4j").spaces2},
            |         ${moduleJson("zio-test", maybeIsTestScope = Some(true)).spaces2}
            |      ]
            |    }
            |  ]
            |}
            |""".stripMargin
        }

        val circeOrgConfig = OrgConfig(
          "circe",
          "io.circe",
          List(
            ModuleConfig("circe-core", isTestScope = false)
          )
        )

        val zioOrgConfig = OrgConfig(
          "zio",
          "dev.zio",
          List(
            ModuleConfig("zio-logging-slf4j", isTestScope = false),
            ModuleConfig("zio-test", isTestScope = true)
          )
        )

        val expected = SearchConfig(
          defaultProductionVersionRegex = ".*",
          maximumPagesToPaginate = 2,
          hotList = List(
            HotListItemConfig("custom", List("circe", "zio")),
            HotListItemConfig("hotCirce", List("circe"))
          ),
          libs = List(
            circeOrgConfig,
            zioOrgConfig
          )
        )
        val actual = SearchConfig.decodeFromString(json)
        assertTrue(
          actual == Right(expected),
          actual.map(_.isValid) == Right(true),
          actual.map(_.errorOrHotListMappings) == Right(
            Right(
              Map(
                "custom" -> List(circeOrgConfig, zioOrgConfig),
                "hotCirce" -> List(circeOrgConfig)
              )
            )
          )
        )
      },
      test("hot list mapping should error when it cannot map") {
        val json = {
          s"""
            |{
            |  "defaultProductionVersionRegex" : "\\\\w",
            |  "maximumPagesToPaginate" : 1,
            |  "retryCount" : 0,
            |  "hotList" : [
            |     {
            |       "name" : "custom",
            |       "refs" : [ "circe", "zio" ]
            |     },{
            |       "name" : "hotCirce",
            |       "refs" : [ "circe"]
            |     }
            |  ],
            |  "libs": [
            |    {
            |      "name": "circe",
            |      "org": "io.circe",
            |      "modules": [
            |         ${moduleJson("circe-core").spaces2}
            |      ]
            |    }
            |  ]
            |}
            |""".stripMargin
        }

        val circeOrgConfig = OrgConfig(
          "circe",
          "io.circe",
          List(
            ModuleConfig("circe-core", isTestScope = false)
          )
        )

        val expected = SearchConfig(
          defaultProductionVersionRegex = "\\w",
          maximumPagesToPaginate = 1,
          hotList = List(
            HotListItemConfig("custom", List("circe", "zio")),
            HotListItemConfig("hotCirce", List("circe"))
          ),
          libs = List(
            circeOrgConfig
          )
        )
        val actual = SearchConfig.decodeFromString(json)
        assertTrue(
          actual == Right(expected),
          actual.map(_.isValid) == Right(true),
          actual.map(_.errorOrHotListMappings) == Right(
            Left(
              "The following lib configs are missing 'zio'"
            )
          )
        )
      }
    )

  private def moduleJson(
      name: String,
      maybeIsTestScope: Option[Boolean] = None,
      maybeVersionPattern: Option[String] = None,
      maybeIsScala: Option[Boolean] = None
  ): Json = {
    Json.fromFields(
      Map(
        "name" -> Some(Json.fromString(name)),
        "isTestScope" -> maybeIsTestScope.map(Json.fromBoolean),
        "versionPattern" -> maybeVersionPattern.map(Json.fromString),
        "isScala" -> maybeIsScala.map(Json.fromBoolean)
      ).collect { case (key, Some(json: Json)) => key -> json }
    )
  }

}
