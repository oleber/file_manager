package com.oleber.filemanager.fileUploader

import java.io.{FileOutputStream, OutputStream}
import java.nio.file.Paths
import com.oleber.filemanager.fileCommons.FileFileCommons
import com.oleber.filemanager.fileUploader.FileFileUploader.FileFileUploaderException
import com.oleber.filemanager.{BlockingFuture, FileUploader}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

object FileFileUploader {

  case class FileFileUploaderException(msg: String) extends Exception(msg)

}

case class FileFileUploader(excludeRegexp: Option[Regex] = None, includeRegexp: Option[Regex] = None) extends FileUploader with FileFileCommons {
  val regex: Regex = "(?:file:)?(.*)".r

  override def open(path: String)(implicit ec: ExecutionContext): Option[Future[OutputStream]] = {
    path match {
      case regex(cleanPath) =>
        val isValidPath = validPath(
          Paths.get(cleanPath).toAbsolutePath.toString,
          excludeRegexp = excludeRegexp,
          includeRegexp = includeRegexp
        )

        if (! isValidPath) {
          throw FileFileUploaderException(s"File $path isn't safe to read")
        }

        Some(BlockingFuture {new FileOutputStream(cleanPath)})
      case _ =>
        None
    }
  }
}