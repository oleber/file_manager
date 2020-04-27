package com.oleber.filemanager

import java.io.{ByteArrayInputStream, FileInputStream, InputStream}
import java.net.URL

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.Source

class FileDownloaderNotFoundException(msg: String) extends Exception(msg)

trait FileDownloader extends FileWorker[InputStream]

class FileDownloaderGroup(fileOpeners: FileDownloader*) {
  def open(path: String)(implicit ec: ExecutionContext): Future[InputStream] = {
    val inputStreamFtrOpt = fileOpeners
      .foldLeft(None: Option[Future[InputStream]]) {
        case (None, fileWorker) => fileWorker.open(path)
        case (Some(ftr), fileWorker) =>
          Some(ftr.recoverWith { case _ => fileWorker.open(path).getOrElse(ftr) })
      }

    inputStreamFtrOpt.getOrElse(throw new FileDownloaderNotFoundException(s"File downloader not found $path"))
  }

  def doWith[A, IS <: InputStream](path: String, transformer: InputStream => IS = { x: InputStream => x })
                                  (cb: IS => A)
                                  (implicit fc: FileCloserEnvironment[IS, A], ec: ExecutionContext)
  : Future[A] = {
    open(path).map { is => fc.closeOnExit(transformer(is))(cb) }
  }

  def doWithSync[A, IS <: InputStream](
                                        path: String,
                                        transformer: InputStream => IS = { x: InputStream => x },
                                        atMost: Duration = 1.hour,
                                      )
                                      (cb: IS => A)
                                      (implicit fc: FileCloserEnvironment[IS, A], ec: ExecutionContext)
  : A = {
    Await.result(doWith(path, transformer)(cb), atMost)
  }

  def withSource[A, IS <: InputStream](path: String, transformer: InputStream => IS = { x: InputStream => x })
                                      (cb: Source => A)
                                      (implicit fc: FileCloserEnvironment[IS, A], ec: ExecutionContext)
  : Future[A] = {
    doWith(path, transformer) { is => cb(Source.fromInputStream(is)) }
  }

  def slurp[T <: InputStream](
                               path: String,
                               transformer: InputStream => T = { is: InputStream => is }
                             )
                             (implicit ec: ExecutionContext)
  : Future[Array[Byte]] = {
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
          Some(BlockingFuture {
            getClass.getClassLoader.getResource(matchPath).openStream()
          })
        case _ => None
      }
    }
  }

  object URLFileDownloader extends FileDownloader {
    val regex = "(?:http|https|file|jar):.*".r

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

  object FileFileDownloader extends FileDownloader {
    override def open(path: String)(implicit ec: ExecutionContext): Option[Future[InputStream]] = {
      Some(BlockingFuture {
        new FileInputStream(path)
      })
    }
  }

}
