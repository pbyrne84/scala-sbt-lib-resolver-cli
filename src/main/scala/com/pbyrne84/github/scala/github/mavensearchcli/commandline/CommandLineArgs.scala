package com.pbyrne84.github.scala.github.mavensearchcli.commandline

object CommandLineArgs {

  import com.monovore.decline._

  private val hotListCommandLine: Opts[HotListLookupType] = Opts
    .option[String](
      long = "hotlist",
      short = "h",
      help = "A hotList is a list of configured references you want to group, more than one can be comma seperated"
    )
    .map(hotListName => HotListLookupType(hotListName))

  private val moduleGroupCommandLine: Opts[ModuleGroupLookupType] = Opts
    .option[String](
      long = "group",
      short = "g",
      help = "a module group is a set of libs tied to an organisation"
    )
    .map(moduleGroupName => ModuleGroupLookupType(moduleGroupName))

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
    val lookupTypeCommand = hotListCommandLine orElse moduleGroupCommandLine

    (lookupTypeCommand, configPathCommand, enableDebugCommand).mapN { (lookupType, config, enableDebug) =>
      CommandLineArgs(lookupType, config, enableDebug)
    }
  }

  def fromCliArgs(args: List[String]): Either[Help, CommandLineArgs] = {
    Command("", "")(commandLineArgs).parse(args.filter(_ != ""))
  }
}

case class CommandLineArgs(lookup: LookupType, configOption: String, enableDebug: Boolean)
