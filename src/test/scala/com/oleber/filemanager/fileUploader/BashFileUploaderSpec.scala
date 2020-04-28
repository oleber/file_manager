package com.oleber.filemanager.fileUploader

import java.io.PrintStream
import java.util.zip.GZIPInputStream
import com.oleber.filemanager.FileCloserEnvironment.closeOnExit
import com.oleber.filemanager.FileDownloader.fileDownloaderGroup
import com.oleber.filemanager.FileUploader.allFileUploaderGroup
import com.oleber.filemanager.FileUtils.withTempDirectory
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

class BashFileUploaderSpec(implicit ee: ExecutionEnv) extends Specification {
  "BashFileUploader" should {
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
