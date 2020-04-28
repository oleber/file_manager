package com.oleber.filemanager.fileDownloader

import java.io.{FileInputStream, InputStream}
import java.nio.file.Paths

import com.oleber.filemanager.fileDownloader.FileFileDownloader.FileFileDownloaderException
import com.oleber.filemanager.{BlockingFuture, FileDownloader}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

object FileFileDownloader {

  case class FileFileDownloaderException(msg: String) extends Exception(msg)

}

case class FileFileDownloader(includeRegexp: Option[Regex] = None, excludeRegexp: Option[Regex] = None) extends FileDownloader {
  val regex: Regex = "(?:file:)?(.*)".r

  override def open(path: String)(implicit ec: ExecutionContext): Option[Future[InputStream]] = {
    path match {
      case regex(cleanPath) =>
        val absolutePath = Paths.get(cleanPath).toAbsolutePath.toString
        includeRegexp.foreach { r =>
          if (!r.pattern.matcher(absolutePath).matches())
            throw FileFileDownloaderException(s"File $absolutePath isn't safe to read")
        }

        excludeRegexp.foreach { r =>
          if (r.pattern.matcher(absolutePath).matches())
            throw FileFileDownloaderException(s"File $absolutePath isn't safe to read")
        }

        Some(BlockingFuture {
          new FileInputStream(cleanPath)
        })
      case _ =>
        None
    }
  }
}
