package com.pbyrne84.github.scala.github.mavensearchcli.commandline

import com.pbyrne84.github.scala.github.mavensearchcli.config.{
  ConfigDefaults,
  DefaultScalaVersion,
  ScalaVersion212,
  ScalaVersion213,
  ScalaVersion3,
  SearchConfig
}
import com.pbyrne84.github.scala.github.mavensearchcli.shared.BaseSpec
import com.pbyrne84.github.scala.github.mavensearchcli.shared.BaseSpec.SharedDeps
import zio.Scope
import zio.test._

object CommandLineArgsSpec extends BaseSpec {

  override def spec: Spec[SharedDeps with TestEnvironment with Scope, Any] =
    suite(getClass.getSimpleName)(
      suite("fromCliArgs should")(
        test("parse an empty command line args and default to the internal config default of the hotList") {
          val args = "--config  internal".split(" ").toList
          val expected =
            Right(CommandLineArgs(DefaultHotListLookupType, "internal", enableDebug = false, DefaultScalaVersion))
          val actual = CommandLineArgs.fromCliArgs(args)

          assertTrue(actual == expected)
        },
        test("parse a hotList command line args with no debug") {
          val args = "--hotlist test-single --config  internal".split(" ").toList
          val expected = Right(
            CommandLineArgs(
              CustomHotListLookupType("test-single"),
              "internal",
              enableDebug = false,
              DefaultScalaVersion
            )
          )
          val actual = CommandLineArgs.fromCliArgs(args)

          assertTrue(actual == expected)
        },
        test("parse a hotList command line args with debug logging") {
          val args = "--hotlist test-single --config internal --debug".split(" ").toList
          val expected = Right(
            CommandLineArgs(CustomHotListLookupType("test-single"), "internal", enableDebug = true, DefaultScalaVersion)
          )
          val actual = CommandLineArgs.fromCliArgs(args)

          assertTrue(actual == expected)
        },
        test("parse a module group command line args with no debug") {
          val args = "--group zio --config internal".split(" ").toList
          val expected =
            Right(CommandLineArgs(ModuleGroupLookupType("zio"), "internal", enableDebug = false, DefaultScalaVersion))
          val actual = CommandLineArgs.fromCliArgs(args)

          assertTrue(actual == expected)
        },
        test("parse a module group command line args with debug logging") {
          val args = "--group zio --config internal --debug".split(" ").toList
          val expected =
            Right(CommandLineArgs(ModuleGroupLookupType("zio"), "internal", enableDebug = true, DefaultScalaVersion))
          val actual = CommandLineArgs.fromCliArgs(args)

          assertTrue(actual == expected)
        },
        test("parse a custom scala version") {
          val args = "--config internal --version 3".split(" ").toList
          val expected =
            Right(CommandLineArgs(DefaultHotListLookupType, "internal", enableDebug = false, ScalaVersion3))
          val actual = CommandLineArgs.fromCliArgs(args)

          assertTrue(actual == expected)
        }
      ),
      suite("getScalaVersion should")(
        test("return configured default from search config if that is required") {
          val versions = List(
            ScalaVersion212,
            ScalaVersion213,
            ScalaVersion3
          )
          assertTrue {
            versions.forall { version =>
              val actual =
                CommandLineArgs(DefaultHotListLookupType, "", enableDebug = false, scalaVersion = DefaultScalaVersion)
                  .getScalaVersion(
                    SearchConfig(ConfigDefaults(defaultScalaVersion = version), 0, List.empty, List.empty)
                  )

              actual == version
            }
          }
        },
        test("return version from the command line args if not requiring default") {
          val versions = List(
            ScalaVersion212,
            ScalaVersion213,
            ScalaVersion3
          )
          assertTrue {
            versions.forall { version =>
              val actual =
                CommandLineArgs(DefaultHotListLookupType, "", enableDebug = false, scalaVersion = version)
                  .getScalaVersion(
                    SearchConfig(ConfigDefaults(defaultScalaVersion = ScalaVersion212), 0, List.empty, List.empty)
                  )

              actual == version
            }
          }
        }
      )
    )

}
