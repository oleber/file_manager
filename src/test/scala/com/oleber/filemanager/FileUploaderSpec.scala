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

      val writeFtr = fileUploaderGroup.doWith(file.toString) { outputStream =>
        closeOnExit(new PrintStream(outputStream)) { printStream =>
          printStream.print("some text")
        }
        2
      }

      val readBufferFtr = writeFtr
        .flatMap { result =>
          result must_=== 2
          fileDownloaderGroup.slurp(file.toString)
        }

      readBufferFtr.map { buffer => new String(buffer) must_=== "some text" }
    }

    "doWith GZip OutputStream" in withTempDirectory[Future[MatchResult[String]]] { path =>
      val file = path.resolve("foo.txt")
      val body = "some text: João"

      for {
        result <- fileUploaderGroup.doWith(file.toString, os => new GZIPOutputStream(os)) { outputStream =>
          closeOnExit(new PrintStream(outputStream)) {
            _.print(body)
          }
          2
        }

        bufferUngzip <- fileDownloaderGroup.slurp(file.toString, is => new GZIPInputStream(is))
        bufferGZip <- fileDownloaderGroup.slurp(file.toString)
      } yield {
        result must_=== 2
        new String(bufferUngzip) must_=== body
        Source.fromInputStream(new GZIPInputStream(new ByteArrayInputStream(bufferGZip))).mkString must_=== body
      }
    }
  }
}
