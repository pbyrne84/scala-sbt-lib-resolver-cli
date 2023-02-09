package com.pbyrne84.github.scala.github.mavensearchcli.error

import com.pbyrne84.github.scala.github.mavensearchcli.config.ValidScalaVersion

sealed abstract class CliException(message: String, maybeCause: Option[Throwable] = None)
    extends RuntimeException(message, maybeCause.orNull)

case class ConfigReaderException(message: String, cause: Throwable) extends CliException(message, Some(cause))

case class SearchConfigDecodingException(message: String, cause: Throwable) extends CliException(message, Some(cause))

case class InvalidScalaVersionException(passedVersion: String)
    extends CliException(ValidScalaVersion.createInvalidScalaVersionMessage(passedVersion))

case class MissingHotListException(message: String) extends CliException(message)

sealed abstract class SingleSearchException(message: String, cause: Throwable)
    extends CliException(message, Some(cause))

case class NetworkSingleSearchException(invalidUrl: String, cause: Throwable)
    extends SingleSearchException(s"The url '$invalidUrl' failed with ${cause.getMessage}", cause)

case class JsonDecodingSingleSearchException(cause: Throwable) extends SingleSearchException(cause.getMessage, cause)

case class UnexpectedSingleSearchException(cause: Throwable) extends SingleSearchException(cause.getMessage, cause)
