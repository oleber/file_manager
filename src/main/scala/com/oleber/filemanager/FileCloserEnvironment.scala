package com.oleber.filemanager

import scala.concurrent.{ExecutionContext, Future}

abstract class FileCloserEnvironment[A: FileCloser, B] {
  def close(a: A)(cb: A => B)(implicit ec: ExecutionContext): B
}

trait LowPriorityFileCloserEnvironment {
  class SyncFileCloserEnvironment[A: FileCloser, B]() extends FileCloserEnvironment[A, B]{
    override def close(a: A)(cb: A => B)(implicit ec: ExecutionContext): B = {
      try {
        cb(a)
      } finally {
        implicitly[FileCloser[A]].close(a)
      }
    }
  }
  implicit def syncFileCloserEnvironment[A: FileCloser, B]: FileCloserEnvironment[A, B] = {
    new SyncFileCloserEnvironment[A, B]()
  }
}

object FileCloserEnvironment extends LowPriorityFileCloserEnvironment {
  class AsyncFileCloserEnvironment[A: FileCloser, B]() extends FileCloserEnvironment[A, Future[B]]{
    override def close(a: A)(cb: A => Future[B])(implicit ec: ExecutionContext): Future[B] = {
      cb(a)
        .map { case b =>
          implicitly[FileCloser[A]].close(a)
          b
        }
    }
  }

  implicit def asyncFileCloserEnvironment[A: FileCloser, B]: FileCloserEnvironment[A, Future[B]] = {
    new AsyncFileCloserEnvironment[A, B]()
  }

  def closeOnExit[A: FileCloser, B](a:A)
                                   (cb: A => B)
                                   (implicit fileCloserEnvironment: FileCloserEnvironment[A, B], ec: ExecutionContext)
  : B = {
    fileCloserEnvironment.close(a)(cb)
  }
}

