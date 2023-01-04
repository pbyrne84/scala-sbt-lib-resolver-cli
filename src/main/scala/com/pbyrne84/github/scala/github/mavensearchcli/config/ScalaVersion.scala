package com.pbyrne84.github.scala.github.mavensearchcli.config

sealed abstract class ScalaVersion(val suffix: String)
case object ScalaVersion212 extends ScalaVersion("2.12")
case object ScalaVersion213 extends ScalaVersion("2.13")
case object ScalaVersion3 extends ScalaVersion("3")
