package com.jcranky.godmode.actions.doobie_action

import cats.effect.Async
import cats.syntax.apply._
import cats.syntax.flatMap._
import doobie.implicits._
import doobie.util.composite.Composite
import doobie.util.fragment.Fragment
import doobie.util.log.LogHandler
import doobie.util.transactor.Transactor

@deprecated("Use DoobieWrappers.query instead", "0.1-M9")
case class DoobieQueryAction[F[_]](fragment: Fragment, failIfEmpty: Boolean = false, logQueries: Boolean = false) {

  def compile[T: Composite](xa: Transactor[F])(implicit F: Async[F]): F[List[T]] = {
    implicit val logHandler: LogHandler =
      if (logQueries) LogHandler.jdkLogHandler else LogHandler.nop

    val q = fragment.query.to[List]

    val transaction: F[List[T]] =
      if (failIfEmpty)
        q.transact(xa).flatMap {
          case Nil => F.raiseError(new RuntimeException("[DoobieQueryAction] Got empty list as result and failIfEmpty is set to true"))
          case list => F.pure(list)
        }
      else
        q.transact(xa)

    F.delay(println(s"[DoobieQueryAction] $fragment")) *> transaction
  }
}
