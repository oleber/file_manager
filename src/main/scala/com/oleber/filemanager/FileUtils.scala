package com.oleber.filemanager

import java.nio.file.{Files, Path}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

object FileUtils {

  def walk(path: Path, directoryFirst: Boolean = false): Iterator[Path] = {
    val subPaths = Files
      .list(path)
      .iterator()
      .asScala
      .flatMap { subPath =>
        if (subPath.toFile.isDirectory) {
          walk(subPath, directoryFirst)
        } else {
          Seq(subPath).iterator
        }
      }

    if (directoryFirst) {
      Seq(path).iterator ++ subPaths
    } else {
      subPaths ++ Seq(path).iterator
    }
  }

  case class TempDirectoryDeleter(path: Path) extends AutoCloseable {
    override def close(): Unit = {
      walk(path)
        .foreach(path => Files.delete(path))
    }
  }

  def withTempDirectory[A](cb: Path => A)
                          (implicit ec: ExecutionContext, fce: FileCloserEnvironment[TempDirectoryDeleter, A])
  : A = {
    FileCloserEnvironment
      .closeOnExit(TempDirectoryDeleter(Files.createTempDirectory(null))) { tdd =>
        cb(tdd.path)
      }
  }
}
