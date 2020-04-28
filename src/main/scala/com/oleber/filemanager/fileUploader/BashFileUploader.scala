package com.oleber.filemanager.fileUploader

import java.io.{FilterOutputStream, OutputStream}

import com.oleber.filemanager.{BlockingFuture, FileUploader}

import scala.concurrent.{ExecutionContext, Future, blocking}
import scala.util.matching.Regex

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