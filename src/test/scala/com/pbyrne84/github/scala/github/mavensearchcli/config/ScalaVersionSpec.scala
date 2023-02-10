package com.pbyrne84.github.scala.github.mavensearchcli.config

import cats.data.Validated
import com.pbyrne84.github.scala.github.mavensearchcli.shared.BaseSpec
import com.pbyrne84.github.scala.github.mavensearchcli.shared.BaseSpec.SharedDeps
import zio.Scope
import zio.test._

object ScalaVersionSpec extends BaseSpec {
  override def spec: Spec[SharedDeps with TestEnvironment with Scope, Any] =
    suite(getClass.getSimpleName)(
      suite("fromStringValue should")(
        test("map all values from their command line text counterparts") {
          val validVersions = List(ScalaVersion212, ScalaVersion213, ScalaVersion3).map(validVersion =>
            validVersion.versionText -> validVersion
          )

          assertTrue {
            validVersions.forall { case (text, expectedVersion) =>
              ValidScalaVersion.fromStringValue(text) == Validated.valid(expectedVersion)
            }
          }
        },
        test("fail on an invalid value") {
          assertTrue(
            ValidScalaVersion.fromStringValue("2.11") == Validated.invalidNel(
              "Scala version '2.11' is unknown, possible values are 2.12, 2.13, 3"
            )
          )
        }
      )
    )
}
