package com.oleber.filemanager.fileDownloader

import java.io.InputStream
import java.net.URL

import com.oleber.filemanager.{BlockingFuture, FileDownloader}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

object URLFileDownloader extends FileDownloader {
  val regex: Regex = "(?:http|https|jar):.*".r

  override def open(path: String)(implicit ec: ExecutionContext): Option[Future[InputStream]] = {
    path match {
      case regex() =>
        Some(BlockingFuture {
          new URL(path).openStream()
        })
      case _ =>
        None
    }
  }
}
