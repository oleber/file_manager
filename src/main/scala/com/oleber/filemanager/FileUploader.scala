package com.oleber.filemanager

import java.io.{FileInputStream, FileOutputStream, FilterOutputStream, OutputStream}
import java.nio.file.Paths

import com.oleber.filemanager.FileDownloader.FileFileDownloader.FileFileDownloaderException
import com.oleber.filemanager.FileUploader.FileFileUploader.FileFileUploaderException

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future, blocking}
import scala.util.matching.Regex


class FileUploaderNotFoundException(msg: String) extends Exception(msg)

class FileUploaderGroup(val fileUploaders: FileUploader*) {
  def open(path: String)(implicit ec: ExecutionContext): Future[OutputStream] = {
    val outputStreamFtrOpt = fileUploaders.foldLeft(None: Option[Future[OutputStream]]) {
      case (None, fileWorker) => fileWorker.open(path)
      case (Some(ftr), fileWorker) =>
        Some(ftr.recoverWith { case _ => fileWorker.open(path).getOrElse(ftr) })
    }

    outputStreamFtrOpt.getOrElse(throw new FileDownloaderNotFoundException(s"File uploader not found $path"))
  }

  def doWith[A, T <: OutputStream](path: String, transformer: OutputStream => T = { x: OutputStream => x })
                                  (cb: T => A)
                                  (implicit fc: FileCloserEnvironment[T, A], ec: ExecutionContext)
  : Future[A] = {
    open(path).map { t => fc.closeOnExit(transformer(t))(cb) }
  }

  def doWithSync[A, OS <: OutputStream](
                                         path: String,
                                         transformer: OutputStream => OS = { x: OutputStream => x },
                                         atMost: Duration = 1.hour
                                       )
                                       (cb: OS => A)
                                       (implicit fc: FileCloserEnvironment[OS, A], ec: ExecutionContext)
  : A = {
    Await.result(doWith(path, transformer)(cb), atMost)
  }
}

trait FileUploader extends FileWorker[OutputStream]

object FileUploader {
  val fileUploaderGroup = new FileUploaderGroup(FileFileUploader())

  val allFileUploaderGroup = new FileUploaderGroup(Seq(BashFileUploader) ++ fileUploaderGroup.fileUploaders: _*)

  object BashFileUploader extends FileUploader {
    val regex: Regex = "bash:(.*)".r

    override def open(path: String)(implicit ec: ExecutionContext): Option[Future[OutputStream]] = {
      path match {
        case regex(command) =>
          Some(BlockingFuture {
            val processBuilder = new java.lang.ProcessBuilder("bash", "-c", command)
            val process = processBuilder.start()
            new FilterOutputStream(process.getOutputStream) {
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

  object FileFileUploader {

    case class FileFileUploaderException(msg: String) extends Exception(msg)

  }

  case class FileFileUploader(pathRegexOpt: Option[Regex] = None) extends FileUploader {
    val regex: Regex = "(?:file:)?(.*)".r

    override def open(path: String)(implicit ec: ExecutionContext): Option[Future[OutputStream]] = {
      path match {
        case regex(cleanPath) =>
          Some(BlockingFuture {
            pathRegexOpt match {
              case Some(regex) =>
                Paths.get(cleanPath).toAbsolutePath.toString match {
                  case regex() =>
                    new FileOutputStream(cleanPath)
                  case absolutePath =>
                    throw FileFileUploaderException(s"File $absolutePath isn't safe to write")
                }
              case None =>
                new FileOutputStream(cleanPath)
            }
          })
        case _ =>
          None
      }
    }
  }


}
