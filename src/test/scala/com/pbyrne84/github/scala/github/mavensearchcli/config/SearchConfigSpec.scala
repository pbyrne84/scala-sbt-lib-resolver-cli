package com.pbyrne84.github.scala.github.mavensearchcli.config

import com.pbyrne84.github.scala.github.mavensearchcli.error.MissingHotListException
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
        val json = jsonOps.formattedJsonUnsafe {
          s"""
            |{
            |  "defaults" : {
            |     "productionVersionRegex" : ".*",
            |     "hotList" : "custom"
            |  },
            |  "defaultProductionVersionRegex" : ".*",
            |  "maximumPagesToPaginate" : 2,
            |  "hotLists" : [
            |     {
            |       "name" : "custom",
            |       "refs" : [ "circe", "zio" ]
            |     },{
            |       "name" : "hotCirce",
            |       "refs" : [ "circe"]
            |     }
            |  ],
            |  "groups": [
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
            |         ${moduleJson("zio-test", maybeModuleType = Some(ScalaTestScope)).spaces2}
            |      ]
            |    }
            |  ]
            |}
            |""".stripMargin
        }

        val circeOrgConfig = GroupConfig(
          "circe",
          "io.circe",
          List(
            ModuleConfig("circe-core", moduleType = ScalaNormalScope)
          )
        )

        val zioOrgConfig = GroupConfig(
          "zio",
          "dev.zio",
          List(
            ModuleConfig("zio-logging-slf4j", moduleType = ScalaNormalScope),
            ModuleConfig("zio-test", moduleType = ScalaTestScope)
          )
        )

        val expected = SearchConfig(
          defaults = ConfigDefaults(productionVersionRegex = ".*", maybeHotList = Some("custom")),
          maximumPagesToPaginate = 2,
          hotLists = List(
            HotListItemConfig("custom", List("circe", "zio")),
            HotListItemConfig("hotCirce", List("circe"))
          ),
          groups = List(
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
            |  "defaults" : {
            |     "productionVersionRegex" :  "\\\\w"
            |   },
            |  "maximumPagesToPaginate" : 1,
            |  "retryCount" : 0,
            |  "hotLists" : [
            |     {
            |       "name" : "custom",
            |       "refs" : [ "circe", "zio" ]
            |     },{
            |       "name" : "hotCirce",
            |       "refs" : [ "circe"]
            |     }
            |  ],
            |  "groups": [
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

        val circeOrgConfig = GroupConfig(
          "circe",
          "io.circe",
          List(
            ModuleConfig("circe-core", moduleType = ScalaNormalScope)
          )
        )

        val expected = SearchConfig(
          defaults = ConfigDefaults(productionVersionRegex = "\\w"),
          maximumPagesToPaginate = 1,
          hotLists = List(
            HotListItemConfig("custom", List("circe", "zio")),
            HotListItemConfig("hotCirce", List("circe"))
          ),
          groups = List(
            circeOrgConfig
          )
        )
        val actual = SearchConfig.decodeFromString(json)
        assertTrue(
          actual == Right(expected),
          actual.map(_.isValid) == Right(true),
          actual.map(_.errorOrHotListMappings.mapErrorToClass) ==
            Right(Left(classOf[MissingHotListException]))
        )
      }
    )

  private def moduleJson(
      name: String,
      maybeModuleType: Option[ModuleType] = None,
      maybeVersionPattern: Option[String] = None
  ): Json = {
    Json.fromFields(
      Map(
        "name" -> Some(Json.fromString(name)),
        "moduleType" -> maybeModuleType.map(moduleType => Json.fromString(moduleType.textValue)),
        "versionPattern" -> maybeVersionPattern.map(Json.fromString)
      ).collect { case (key, Some(json: Json)) => key -> json }
    )
  }

}
