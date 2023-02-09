package com.pbyrne84.github.scala.github.mavensearchcli.commandline

import com.pbyrne84.github.scala.github.mavensearchcli.shared.BaseSpec
import com.pbyrne84.github.scala.github.mavensearchcli.shared.BaseSpec.SharedDeps
import zio.Scope
import zio.test._

object CommandLineArgsSpec extends BaseSpec {

  override def spec: Spec[SharedDeps with TestEnvironment with Scope, Any] =
    suite(getClass.getSimpleName)(
      suite("fromCliArgs")(
        test("should parse empty command line args and default to the internal config default of the hotList") {
          val args = "--config  internal".split(" ").toList
          val expected = Right(CommandLineArgs(DefaultHotListLookupType, "internal", enableDebug = false))
          val actual = CommandLineArgs.fromCliArgs(args)

          assertTrue(actual == expected)
        },
        test("should parse a hotList command line args with no debug") {
          val args = "--hotlist test-single --config  internal".split(" ").toList
          val expected = Right(CommandLineArgs(CustomHotListLookupType("test-single"), "internal", enableDebug = false))
          val actual = CommandLineArgs.fromCliArgs(args)

          assertTrue(actual == expected)
        },
        test("should parse a hotList command line args with debug logging") {
          val args = "--hotlist test-single --config internal --debug".split(" ").toList
          val expected = Right(CommandLineArgs(CustomHotListLookupType("test-single"), "internal", enableDebug = true))
          val actual = CommandLineArgs.fromCliArgs(args)

          assertTrue(actual == expected)
        },
        test("should parse a module group command line args with no debug") {
          val args = "--group zio --config internal".split(" ").toList
          val expected = Right(CommandLineArgs(ModuleGroupLookupType("zio"), "internal", enableDebug = false))
          val actual = CommandLineArgs.fromCliArgs(args)

          assertTrue(actual == expected)
        },
        test("should parse a module group command line args with debug logging") {
          val args = "--group zio --config internal --debug".split(" ").toList
          val expected = Right(CommandLineArgs(ModuleGroupLookupType("zio"), "internal", enableDebug = true))
          val actual = CommandLineArgs.fromCliArgs(args)

          assertTrue(actual == expected)
        }
      )
    )

}
