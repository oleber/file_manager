package com.oleber.filemanager.fileDownloader

import java.io.{ByteArrayInputStream, InputStream}

import com.oleber.filemanager.FileDownloader

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

object StringFileDownloader extends FileDownloader {
  val regExp: Regex = "string:(.*)".r

  override def open(path: String)(implicit ec: ExecutionContext): Option[Future[InputStream]] = {
    path match {
      case regExp(buffer) => Some(Future {
        new ByteArrayInputStream(buffer.getBytes)
      })
      case _ => None
    }
  }
}