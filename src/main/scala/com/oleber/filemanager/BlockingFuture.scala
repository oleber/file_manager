package com.oleber.filemanager

import scala.concurrent.{ExecutionContext, Future, blocking}

object BlockingFuture {
  def apply[T](cf: => T)(implicit ec: ExecutionContext): Future[T] = Future {
    blocking {
      cf
    }
  }
}
