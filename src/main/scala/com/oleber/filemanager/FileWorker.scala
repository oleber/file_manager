package com.oleber.filemanager

import scala.concurrent.{ExecutionContext, Future}

trait FileWorker[T] {
  def open(path: String)(implicit ec: ExecutionContext): Option[Future[T]]
}

class FileWorkerGroup[T: FileCloser](fileWorkers: FileWorker[T]*) {
  def open(path: String)(implicit ec: ExecutionContext): Option[Future[T]] = {
    fileWorkers.foldLeft(None: Option[Future[T]]) {
      case (None, fileWorker) => fileWorker.open(path)
      case (Some(ftr), fileWorker) =>
        Some(ftr.recoverWith { case _ => fileWorker.open(path).getOrElse(ftr) })
    }
  }

  def doWith[A](path: String, transformer: T => T = identity)
               (cb: T => A)
               (implicit fc: FileCloserEnvironment[T, A], ec: ExecutionContext): Option[Future[A]] = {
    open(path)
      .map(ftr =>
        ftr.map { t =>
          implicitly[FileCloserEnvironment[T, A]].close(t) { t =>
            cb(transformer(t))
          }
        }
      )
  }
}



