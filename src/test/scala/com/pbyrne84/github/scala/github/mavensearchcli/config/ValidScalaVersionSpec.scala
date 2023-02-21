package com.pbyrne84.github.scala.github.mavensearchcli.config
import cats.data.Validated
import com.pbyrne84.github.scala.github.mavensearchcli.shared.BaseSpec
import com.pbyrne84.github.scala.github.mavensearchcli.shared.BaseSpec.SharedDeps
import io.circe.Json
import zio.Scope
import zio.test._

object ValidScalaVersionSpec extends BaseSpec {
  override def spec: Spec[SharedDeps with TestEnvironment with Scope, Any] =
    suite(getClass.getSimpleName)(
      suite("fromStringValue should")(
        test("convert valid scala version text") {
          val validVersions = List(
            "2.12",
            "2.13",
            "3"
          )

          val expectedConversion = List(ScalaVersion212, ScalaVersion213, ScalaVersion3)
            .map(Validated.valid)

          assertTrue(validVersions.map(ValidScalaVersion.fromStringValue) == expectedConversion)
        },
        test("fail on invalid scala version text") {
          assertTrue(
            ValidScalaVersion.fromStringValue("3.1") == Validated.invalidNel(
              "Scala version '3.1' is unknown, possible values are 2.12, 2.13, 3"
            )
          )
        }
      ),
      suite("json decoding should")(
        test("convert valid scala version text") {
          val validVersions = List(
            "2.12",
            "2.13",
            "3"
          ).map(Json.fromString)

          val expectedConversion = List(ScalaVersion212, ScalaVersion213, ScalaVersion3)
            .map(Right.apply)

          assertTrue(
            validVersions.map(validVersionJson =>
              ValidScalaVersion.validScalaVersionDecoder.decodeJson(validVersionJson)
            ) == expectedConversion
          )
        },
        test("fail on invalid scala version text") {
          val expectedMessage =
            "com.pbyrne84.github.scala.github.mavensearchcli.error.InvalidScalaVersionException: Scala version 'aaaa' is unknown, possible values are 2.12, 2.13, 3"

          val actual =
            ValidScalaVersion.validScalaVersionDecoder.decodeJson(Json.fromString("aaaa")).left.map {
              _.message.substring(
                0,
                expectedMessage.length
              ) // message returns history of original exception for logging purposes
            }

          assertTrue(
            actual == Left(expectedMessage)
          )
        }
      )
    )
}
