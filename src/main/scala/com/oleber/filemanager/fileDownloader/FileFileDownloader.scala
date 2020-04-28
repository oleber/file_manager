package com.oleber.filemanager.fileDownloader

import java.io.{FileInputStream, InputStream}
import java.nio.file.Paths
import com.oleber.filemanager.fileCommons.FileFileCommons
import com.oleber.filemanager.fileDownloader.FileFileDownloader.FileFileDownloaderException
import com.oleber.filemanager.{BlockingFuture, FileDownloader}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

object FileFileDownloader {

  case class FileFileDownloaderException(msg: String) extends Exception(msg)

}

case class FileFileDownloader(excludeRegexp: Option[Regex] = None, includeRegexp: Option[Regex] = None) extends FileDownloader with FileFileCommons {
  val regex: Regex = "(?:file:)?(.*)".r

  override def open(path: String)(implicit ec: ExecutionContext): Option[Future[InputStream]] = {
    path match {
      case regex(cleanPath) =>
        val isValidPath = validPath(
          Paths.get(cleanPath).toAbsolutePath.toString,
          excludeRegexp = excludeRegexp,
          includeRegexp = includeRegexp
        )

        if (! isValidPath) {
          throw FileFileDownloaderException(s"File $path isn't safe to read")
        }

        Some(BlockingFuture {new FileInputStream(cleanPath)})
      case _ =>
        None
    }
  }
}
