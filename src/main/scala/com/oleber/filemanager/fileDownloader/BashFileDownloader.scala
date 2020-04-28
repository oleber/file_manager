package com.oleber.filemanager.fileDownloader

import java.io.{FilterInputStream, InputStream}

import com.oleber.filemanager.{BlockingFuture, FileDownloader}

import scala.concurrent.{ExecutionContext, Future, blocking}
import scala.util.matching.Regex

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