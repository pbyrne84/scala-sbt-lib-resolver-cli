package com.pbyrne84.github.scala.github.mavensearchcli.config

import com.pbyrne84.github.scala.github.mavensearchcli.shared.BaseSpec
import com.pbyrne84.github.scala.github.mavensearchcli.shared.BaseSpec.SharedDeps
import io.circe.Json
import zio.Scope
import zio.test._

object ModuleTypeSpec extends BaseSpec {
  override def spec: Spec[SharedDeps with TestEnvironment with Scope, Any] =
    suite("ModuleType")(
      suite("decoding")(
        test("should convert from a json string to the object") {
          val actual = List(
            "scala",
            "scala-test",
            "java",
            "java-test",
            "plugin",
            "compiler-plugin"
          ).map { textValue =>
            // this escapes and wraps in ""
            val stringAsJson = Json.fromString(textValue).noSpaces
            io.circe.parser.decode[ModuleType](stringAsJson)
          }

          val expected = List(
            ScalaNormalScope,
            ScalaTestScope,
            JavaNormalScope,
            JavaTestScope,
            SbtPlugin,
            SbtCompilerPlugin
          ).map(Right.apply)

          assertTrue(actual == expected)
        }
      ),
      suite("isScalaVersionedLib")(
        test("should detect if it is a scala lib by object type") {
          // catches if the implementation switches to val as that leads to a hard to debug NullPointerException
          val actual = List(
            ScalaNormalScope,
            ScalaTestScope,
            JavaNormalScope,
            JavaTestScope,
            SbtPlugin,
            SbtCompilerPlugin
          ).map(_.isScalaVersionedLib)

          val expected = List(true, true, false, false, false, true)

          assertTrue(actual == expected)
        }
      )
    )
}
