package com.oleber.filemanager

import java.io.{ByteArrayInputStream, FilterInputStream, InputStream}
import java.net.URL

import com.oleber.filemanager.fileDownloader.FileFileDownloader

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future, blocking}
import scala.io.Source
import scala.util.matching.Regex

class FileDownloaderNotFoundException(msg: String) extends Exception(msg)

trait FileDownloader extends FileWorker[InputStream]

class FileDownloaderGroup(val fileOpeners: FileDownloader*) {
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

  def slurpSync[T <: InputStream](
                                   path: String,
                                   transformer: InputStream => T = { is: InputStream => is },
                                   atMost: Duration = 1.hour,
                                 )
                                 (implicit ec: ExecutionContext)
  : Array[Byte] = {
    Await.result(slurp(path, transformer), atMost)
  }
}

object FileDownloader {

  val fileDownloaderGroup = new FileDownloaderGroup(
    StringFileDownloader,
    ResourceFileDownloader,
    URLFileDownloader,
    FileFileDownloader()
  )

  val allFileDownloaderGroup = new FileDownloaderGroup(Seq(BashFileDownloader) ++ fileDownloaderGroup.fileOpeners: _*)

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

  object URLFileDownloader extends FileDownloader {
    val regex: Regex = "(?:http|https|file|jar):.*".r

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


  object BashFileDownloader extends FileDownloader {
    val regex: Regex = "bash:(.*)".r

    override def open(path: String)(implicit ec: ExecutionContext): Option[Future[InputStream]] = {
      path match {
        case regex(command) =>
          Some(BlockingFuture {
            val processBuilder = new java.lang.ProcessBuilder("bash", "-c", command)
            val process = processBuilder.start()
            new FilterInputStream(process.getInputStream) {
              override def close(): Unit = {
                super.close()
                blocking {
                  process.waitFor()
                }
              }
            }
          })
        case _ => None
      }
    }
  }


}
