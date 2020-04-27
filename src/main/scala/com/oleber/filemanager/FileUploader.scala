package com.oleber.filemanager

import java.io.{FileOutputStream, InputStream, OutputStream}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}


class FileUploaderNotFoundException(msg: String) extends Exception(msg)

class FileUploaderGroup(fileUploaders: FileUploader*) {
  def open(path: String)(implicit ec: ExecutionContext): Future[OutputStream] = {
    val outputStreamFtrOpt = fileUploaders.foldLeft(None: Option[Future[OutputStream]]) {
      case (None, fileWorker) => fileWorker.open(path)
      case (Some(ftr), fileWorker) =>
        Some(ftr.recoverWith { case _ => fileWorker.open(path).getOrElse(ftr) })
    }

    outputStreamFtrOpt.getOrElse(throw new FileDownloaderNotFoundException(s"File uploader not found $path"))
  }

  def doWith[A, T <: OutputStream](path: String, transformer: OutputStream => T = {x: OutputStream => x})
               (cb: T => A)
               (implicit fc: FileCloserEnvironment[T, A], ec: ExecutionContext)
  : Future[A] = {
    open(path).map { t => fc.closeOnExit(transformer(t))(cb)}
  }

  def doWithSync[A, OS <: OutputStream](
                                         path: String,
                                         transformer: OutputStream => OS = { x: OutputStream => x},
                                         atMost: Duration = 1.hour
                                       )
                                       (cb: OS => A)
                                       (implicit fc: FileCloserEnvironment[OS, A], ec: ExecutionContext)
  : A = {
    Await.result(doWith(path, transformer)(cb), atMost)
  }
}

trait FileUploader extends FileWorker[OutputStream]

object FileUploader {
  val fileUploaderGroup = new FileUploaderGroup(FileFileUploader)

  object FileFileUploader extends FileUploader {
    override def open(path: String)(implicit ec: ExecutionContext): Option[Future[OutputStream]] = {
      Some(Future{new FileOutputStream(path)})
    }
  }
}
