package com.oleber.filemanager

import scala.concurrent.{ExecutionContext, Future}

trait FileWorker[T] {
  def open(path: String)(implicit ec: ExecutionContext): Option[Future[T]]
}




