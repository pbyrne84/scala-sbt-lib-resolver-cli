package com.pbyrne84.github.scala.github.mavensearchcli

import zio.ZIO

// ZIO.serviceWithZIO[A] gets a bit finger twisty to type all the time with all the shift key action
trait ZIOServiced[A] {
  protected val serviced: ZIO.ServiceWithZIOPartiallyApplied[A] = ZIO.serviceWithZIO[A]
}
