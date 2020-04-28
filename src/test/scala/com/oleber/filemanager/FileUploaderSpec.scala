package com.oleber.filemanager

import java.io.{ByteArrayInputStream, PrintStream}
import java.nio.file.Path
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

import com.oleber.filemanager.FileCloserEnvironment.closeOnExit
import com.oleber.filemanager.FileDownloader.FileFileDownloader.FileFileDownloaderException
import com.oleber.filemanager.FileDownloader.{FileFileDownloader, StringFileDownloader}
import com.oleber.filemanager.FileUploader.FileFileUploader.FileFileUploaderException
import com.oleber.filemanager.FileUploader.{FileFileUploader, allFileUploaderGroup}
import com.oleber.filemanager.FileUtils.withTempDirectory
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

    "FileFileUploader filter file paths" in withTempDirectory { basePath =>

      def createFile(path: Path) = {
        path.toFile.mkdir()
        path.resolve("some_file.txt")
      }
      val pathReadable = basePath.resolve("readable")
      val pathNotReadable = basePath.resolve("not_readable")

      val localFileUploaderGroup = new FileUploaderGroup(FileFileUploader(Some(s"""\\Q${pathReadable}\\E.*""".r)))

      val filePathReadable = createFile(pathReadable)
      val filePathNotReadable = createFile(pathNotReadable)

      def checkReadable(path: String, body: String) = {
        localFileUploaderGroup.doWithSync(path, os => new PrintStream(os)) {
          _.print(body)
        }
        new String(fileDownloaderGroup.slurpSync(path)).trim must_=== body
      }

      checkReadable(filePathReadable.toUri.toString, "some test uri")
      checkReadable(filePathReadable.toString, "some test raw")
      filePathReadable.toFile.exists() must beTrue

      def checkThrows(path: String) = {
        localFileUploaderGroup.doWithSync(path, os => new PrintStream(os)) {
          _.print("some test")
        } must throwA[FileFileUploaderException]
      }

      checkThrows(filePathNotReadable.toUri.toString)
      checkThrows(filePathNotReadable.toString)
      filePathNotReadable.toFile.exists() must beFalse
    }

  }

}
