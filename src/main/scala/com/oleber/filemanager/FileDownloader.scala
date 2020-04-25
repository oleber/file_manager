package com.oleber.filemanager

import java.io.{ByteArrayInputStream, FileInputStream, InputStream}
import java.net.URL

import scala.concurrent.{ExecutionContext, Future, blocking}

trait FileDownloader extends FileWorker[InputStream]

class FileDownloaderGroup(fileOpeners: FileDownloader*) extends FileWorkerGroup[InputStream](fileOpeners: _*) {
  def slurp[T <: InputStream](path: String, transformer: InputStream => T = {is: InputStream => is})
                             (implicit ec: ExecutionContext)
  : Option[Future[Array[Byte]]] = {
    doWith(path, transformer) { inputStream =>
      import java.io.ByteArrayOutputStream
      val result = new ByteArrayOutputStream
      val buffer = new Array[Byte](1024)
      var length = inputStream.read(buffer)
      while (length != -1) {
        result.write(buffer, 0, length)
        length = inputStream.read(buffer)
      }
      result.toByteArray
    }
  }
}

object FileDownloader {

  val fileDownloaderGroup = new FileDownloaderGroup(
    StringFileDownloader,
    ResourceFileDownloader,
    URLFileDownloader,
    FileFileDownloader
  )

  object StringFileDownloader extends FileDownloader {
    val regExp = "string://(.*)".r

    override def open(path: String)(implicit ec: ExecutionContext): Option[Future[InputStream]] = {
      path match {
        case regExp(buffer) => Some(Future {
          new ByteArrayInputStream(buffer.getBytes)
        })
        case _ => None
      }
    }
  }

  object ResourceFileDownloader extends FileDownloader {
    val regex = "classpath:(.*)".r

    override def open(path: String)(implicit ec: ExecutionContext): Option[Future[InputStream]] = {
      path match {
        case regex(matchPath) =>
          Some( BlockingFuture { getClass.getClassLoader.getResource(matchPath).openStream() } )
        case _ => None
      }
    }
  }

  object URLFileDownloader extends FileDownloader {
    val regex = "(?:http|https|file|jar):.*".r

    override def open(path: String)(implicit ec: ExecutionContext): Option[Future[InputStream]] = {
      path match {
        case regex() =>
          Some(BlockingFuture {new URL(path).openStream()})
        case _ =>
          None
      }
    }
  }

  object FileFileDownloader extends FileDownloader {
    override def open(path: String)(implicit ec: ExecutionContext): Option[Future[InputStream]] = {
      Some(BlockingFuture {new FileInputStream(path)})
    }
  }
}
