package com.pbyrne84.github.scala.github.mavensearchcli.shared

import cats.data.NonEmptyList
import com.pbyrne84.github.scala.github.mavensearchcli.config.CommandLineConfig
import com.pbyrne84.github.scala.github.mavensearchcli.shared.BaseSpec.SharedDeps
import com.pbyrne84.github.scala.github.mavensearchcli.shared.wiremock.MavenWireMock
import zio.test.ZIOSpec
import zio.{ZIO, ZLayer}

object BaseSpec {

  type SharedDeps = MavenWireMock

  val sharedLayer = ZLayer.make[SharedDeps](
    InitialisedPorts.layer,
    MavenWireMock.layer,
    CommandLineConfig.layer
  )

}

abstract class BaseSpec extends ZIOSpec[SharedDeps] {
  override def bootstrap: ZLayer[Any, Any, SharedDeps] = BaseSpec.sharedLayer

  protected def reset: ZIO[MavenWireMock, Throwable, Unit] = {
    MavenWireMock.reset
  }

  // I have a common but bad habit of shoving everything in the base spec of tests.
  // The problem with this is when you want to auto complete stuff it is very hard to resolve
  // as there can be 200 methods starting with get or something. Dividing things by some sort of instance
  // helps alleviate this.
  protected val jsonOps = new JsonOps

  implicit class EitherTestOps[A, B](either: Either[A, B]) {

    /** Useful when all you care about it the type of the error as that can have more value than the text of the error
      * as all coding operations are done on type.
      * @return
      */
    def mapErrorToClass: Either[Class[_ <: A], B] =
      either.left.map(_.getClass)
  }

  implicit class ListOps[A](list: List[A]) {

    /** Sometimes we programmatically generate data, nice to know it is not empty as that can happen by accident.
      * @return
      */
    def asNonEmptyList: Either[RuntimeException, NonEmptyList[A]] = list match {
      case ::(head, next) => Right(NonEmptyList(head, next))
      case Nil => Left(new RuntimeException("No elements found in list"))
    }
  }

}
