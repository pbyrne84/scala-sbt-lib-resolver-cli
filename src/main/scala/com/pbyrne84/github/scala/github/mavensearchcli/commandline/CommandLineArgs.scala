package com.pbyrne84.github.scala.github.mavensearchcli.commandline

import com.pbyrne84.github.scala.github.mavensearchcli.config.{
  DefaultScalaVersion,
  ScalaVersion,
  SearchConfig,
  ValidScalaVersion
}

object CommandLineArgs {

  import com.monovore.decline._

  private val hotListCommandLine: Opts[HotListLookupType] = Opts
    .option[String](
      long = "hotlist",
      short = "h",
      help = "A hotList is a list of configured references you want to group. A default can be set in the config."
    )
    .map(hotListName => CustomHotListLookupType(hotListName))
    .withDefault(DefaultHotListLookupType)

  private val moduleGroupCommandLine: Opts[ModuleGroupLookupType] = Opts
    .option[String](
      long = "group",
      short = "g",
      help = "a module group is a set of libs tied to an organisation"
    )
    .map(moduleGroupName => ModuleGroupLookupType(moduleGroupName))

  private val scalaVersionCommandLine: Opts[ScalaVersion] = Opts
    .option[String](
      long = "version",
      short = "s",
      help = "The version of scala, a default can be set in the config"
    )
    .mapValidated(ValidScalaVersion.fromStringValue)
    .withDefault(DefaultScalaVersion)

  private val configPathCommand = Opts
    .option[String](
      long = "config",
      short = "c",
      help = "Path of the config file"
    )

  private val enableDebugCommand = Opts
    .flag(
      long = "debug",
      short = "d",
      help = "view debug logs"
    )
    .orFalse

  import cats.implicits._

  private val commandLineArgs: Opts[CommandLineArgs] = {
    val lookupTypeCommand = moduleGroupCommandLine orElse hotListCommandLine

    (lookupTypeCommand, configPathCommand, enableDebugCommand, scalaVersionCommandLine).mapN {
      (lookupType, config, enableDebug, commandLineScalaVersion) =>
        CommandLineArgs(lookupType, config, enableDebug, commandLineScalaVersion)
    }
  }

  def fromCliArgs(args: List[String]): Either[Help, CommandLineArgs] = {
    Command("", "")(commandLineArgs).parse(args.filter(_ != ""))
  }
}

case class CommandLineArgs(
    lookup: LookupType,
    configOption: String,
    enableDebug: Boolean,
    private val scalaVersion: ScalaVersion
) {
  def getScalaVersion(searchConfig: SearchConfig): ValidScalaVersion = {
    scalaVersion match {
      case DefaultScalaVersion => searchConfig.defaults.defaultScalaVersion
      case version: ValidScalaVersion => version
    }
  }

}
