package com.oleber.filemanager.fileDownloader

import java.io.{FileOutputStream, PrintStream}
import java.nio.file.Path

import com.oleber.filemanager.FileCloserEnvironment.closeOnExit
import com.oleber.filemanager.FileDownloaderGroup
import com.oleber.filemanager.FileUtils.withTempDirectory
import com.oleber.filemanager.fileDownloader.FileFileDownloader.FileFileDownloaderException
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

class FileFileDownloaderSpec(implicit ee: ExecutionEnv) extends Specification {
  "FileFileDownloader" should {
    val body = "some text: João"
    def createFile(path: Path)= {
      path.toFile.mkdirs()
      val filePath = path.resolve("file.txt")
      closeOnExit(new PrintStream(new FileOutputStream(filePath.toFile))) {_.println(body)}

      filePath
    }

    "exclude path" in withTempDirectory { path =>
      // given
      val readablePath = path.resolve("readable")
      val notReadablePath = path.resolve(".not_readable")

      val readableFilePath = createFile(readablePath)
      val notReadableFilePath = createFile(notReadablePath)

      val fileDownloaderGroup = new FileDownloaderGroup(FileFileDownloader(excludeRegexp = Some(s"/\\.".r)))

      // then
      new String(fileDownloaderGroup.slurpSync(notReadableFilePath.toString)) must throwA[FileFileDownloaderException]
      new String(fileDownloaderGroup.slurpSync(readableFilePath.toString)).trim must_=== body
    }

    "include path" in withTempDirectory { path =>
      // given
      val readablePath = path.resolve("readable")
      val notReadablePath = path.resolve("not_readable")

      val readableFilePath = createFile(readablePath)
      val notReadableFilePath = createFile(notReadablePath)

      val fileDownloaderGroup = new FileDownloaderGroup(FileFileDownloader(includeRegexp = Some(s"""\\Q$readablePath\\E/.*""".r)))

      // then
      new String(fileDownloaderGroup.slurpSync(notReadableFilePath.toString)) must throwA[FileFileDownloaderException]
      new String(fileDownloaderGroup.slurpSync(readableFilePath.toString)).trim must_=== body
    }
  }
}
