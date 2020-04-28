package com.oleber.filemanager

import java.io.{ByteArrayInputStream, PrintStream}
import java.nio.file.Path
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

import com.oleber.filemanager.FileCloserEnvironment.closeOnExit
import com.oleber.filemanager.FileUploader.allFileUploaderGroup
import com.oleber.filemanager.FileUtils.withTempDirectory
import com.oleber.filemanager.fileUploader.FileFileUploader
import com.oleber.filemanager.fileUploader.FileFileUploader.FileFileUploaderException
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

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

    "doWith GZip OutputStream" in withTempDirectory { path =>
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

    "BashFileUploader" in withTempDirectory { path =>
      val file = path.resolve("foo.txt")
      val command = s"bash: cat | gzip > $file"
      val body = "some text: João"

      for {
        result <- allFileUploaderGroup.doWith(command) { outputStream =>
          closeOnExit(new PrintStream(outputStream)) {
            _.print(body)
          }
          2
        }

        buffer <- fileDownloaderGroup.slurp(file.toString, is => new GZIPInputStream(is))
      } yield {
        result must_=== 2
        new String(buffer) must_=== body
      }
    }
  }
}
