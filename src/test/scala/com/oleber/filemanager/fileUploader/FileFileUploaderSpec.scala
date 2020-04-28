package com.oleber.filemanager.fileUploader

import java.io.{File, PrintStream}
import java.nio.file.Path

import com.oleber.filemanager.FileDownloader.fileDownloaderGroup
import com.oleber.filemanager.FileUploader.fileUploaderGroup
import com.oleber.filemanager.FileUploaderGroup
import com.oleber.filemanager.FileUtils.withTempDirectory
import com.oleber.filemanager.fileUploader.FileFileUploader.FileFileUploaderException
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

class FileFileUploaderSpec(implicit ee: ExecutionEnv) extends Specification {
  "FileFileUploader" should {
    def createFile(path: Path) = {
      path.toFile.mkdir()
      path.resolve("some_file.txt").toString
    }

    "read file" in withTempDirectory { path =>
      val filePath = createFile(path)
      fileUploaderGroup.doWithSync(filePath, os => new PrintStream(os)){os => os.println("some João")}
      new String(fileDownloaderGroup.slurpSync(filePath)).trim must_=== "some João"
    }

    "exclude path" in withTempDirectory { path =>
      // given
      val readablePath = path.resolve("readable")
      val notReadablePath = path.resolve(".not_readable")

      val readableFilePath = createFile(readablePath)
      val notReadableFilePath = createFile(notReadablePath)

      val fileUploaderGroup = new FileUploaderGroup(FileFileUploader(excludeRegexp = Some(s"""\\Q${notReadablePath}\\E""".r)))

      def write(path:String) = {
        fileUploaderGroup.doWithSync(path, os => new PrintStream(os)){os => os.println("some João"); 2}
      }

      write(notReadableFilePath) must throwA[FileFileUploaderException]
      new File(notReadableFilePath).isFile must beFalse

      write(readableFilePath) must_=== 2
      new String(fileDownloaderGroup.slurpSync(readableFilePath)).trim must_=== "some João"
    }

    "include path" in withTempDirectory { path =>
      // given
      val readablePath = path.resolve("readable")
      val notReadablePath = path.resolve(".not_readable")

      val readableFilePath = createFile(readablePath)
      val notReadableFilePath = createFile(notReadablePath)

      val fileUploaderGroup = new FileUploaderGroup(FileFileUploader(includeRegexp = Some(s"""\\Q${readablePath}\\E""".r)))

      def write(path:String) = {
        fileUploaderGroup.doWithSync(path, os => new PrintStream(os)){os => os.println("some João"); 2}
      }

      write(notReadableFilePath) must throwA[FileFileUploaderException]
      new File(notReadableFilePath).isFile must beFalse

      write(readableFilePath) must_=== 2
      new String(fileDownloaderGroup.slurpSync(readableFilePath)).trim must_=== "some João"
    }
  }
}
