package com.oleber.filemanager.fileDownloader

import java.io.InputStream

import com.oleber.filemanager.{BlockingFuture, FileDownloader}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

object ResourceFileDownloader extends FileDownloader {
  val regex: Regex = "classpath:(.*)".r

  override def open(path: String)(implicit ec: ExecutionContext): Option[Future[InputStream]] = {
    path match {
      case regex(matchPath) =>
        Some(BlockingFuture {
          getClass.getClassLoader.getResource(matchPath).openStream()
        })
      case _ => None
    }
  }
}

