package com.oleber.filemanager

import java.io.{ByteArrayInputStream, PrintStream}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

import com.oleber.filemanager.FileCloserEnvironment.closeOnExit
import com.oleber.filemanager.FileUtils.withTempDirectory
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification

import scala.concurrent.Future
import scala.io.Source

class FileUploaderSpec(implicit ee: ExecutionEnv) extends Specification {

  import FileDownloader.fileDownloaderGroup
  import FileUploader.fileUploaderGroup

  "FileUploader" should {
    "doWith" in withTempDirectory { path =>
      val file = path.resolve("foo.txt")

      val Some(writeFtr) = fileUploaderGroup.doWith(file.toString) { outputStream =>
        closeOnExit(new PrintStream(outputStream)) { printStream =>
          printStream.print("some text")
        }
        2
      }

      val readBufferFtr = writeFtr
        .flatMap { result =>
          result must_=== 2
          val Some(bufferFtr) = fileDownloaderGroup.slurp(file.toString)
          bufferFtr
        }

      readBufferFtr.map { buffer => new String(buffer) must_=== "some text" }
    }

    "doWith GZip OutputStream" in withTempDirectory[Future[MatchResult[String]]] { path =>
      val file = path.resolve("foo.txt")

      val Some(writeFtr) = fileUploaderGroup.doWith(file.toString, os => new GZIPOutputStream(os)) { outputStream =>
        closeOnExit(new PrintStream(outputStream)) {
          _.print("some text")
        }
        2
      }

      val readBufferUngzipFtr = writeFtr.flatMap { result =>
        result must_=== 2
        val Some(bufferFtr) = fileDownloaderGroup.slurp(file.toString, is => new GZIPInputStream(is))
        bufferFtr
      }
      readBufferUngzipFtr.map { buffer => new String(buffer) must_=== "some text" }

      val readBufferGzipFtr = writeFtr.flatMap { result =>
        result must_=== 2
        val Some(bufferFtr) = fileDownloaderGroup.slurp(file.toString)
        bufferFtr
      }

      readBufferGzipFtr
        .map { buffer =>
          val source = Source.fromInputStream(new GZIPInputStream(new ByteArrayInputStream(buffer)))
          source.mkString must_=== "some text"
        }
    }
  }
}
