package com.oleber.filemanager

import java.io.{FileOutputStream, OutputStream}

import scala.concurrent.{ExecutionContext, Future}

class FileUploaderGroup(fileUploaders: FileUploader*) extends FileWorkerGroup[OutputStream](fileUploaders: _*)

trait FileUploader extends FileWorker[OutputStream]

object FileUploader {
  val fileUploaderGroup = new FileUploaderGroup(FileFileUploader)

  object FileFileUploader extends FileUploader {
    override def open(path: String)(implicit ec: ExecutionContext): Option[Future[OutputStream]] = {
      Some(Future{new FileOutputStream(path)})
    }
  }
}
