package com.oleber.filemanager

import java.io.{File, PrintStream}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

import com.oleber.filemanager.FileDownloader.allFileDownloaderGroup
import com.oleber.filemanager.FileUploader.fileUploaderGroup
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import scala.io.Source

class FileDownloaderSpec(implicit ee: ExecutionEnv) extends Specification {

  import FileDownloader.fileDownloaderGroup

  "FileManager" should {

    "FileWorkerGroup basic" in {
      val ftr = fileDownloaderGroup.doWith("string:foo") { is =>
        Source.fromInputStream(is).getLines().mkString
      }

      ftr.map(_ must be_===("foo")).await
    }

    "slurp" in {
      val ftr = allFileDownloaderGroup.slurp("string:foo")

      ftr.map(array => new String(array) must be_===("foo")).await
    }

    "FileDownloader Gzip" in {
      val file = File.createTempFile("temp", null)
      file.deleteOnExit()

      fileUploaderGroup.doWithSync(
        file.toString,
        transformer = { os => new PrintStream(new GZIPOutputStream(os)) }
      ) {
        _.print("some text: João")
      }

      val ftr = fileDownloaderGroup.slurp(file.toURI.toString, { is => new GZIPInputStream(is) })

      ftr.map(body => new String(body) must_=== "some text: João").await
    }

    "withSource" in {
      val body =
        """
          |some text:
          | João
          |""".stripMargin
      val file = File.createTempFile("temp", null)
      file.deleteOnExit()

      fileUploaderGroup.doWithSync(
        file.toString,
        transformer = { os => new PrintStream(new GZIPOutputStream(os)) }
      ) {
        _.print(body)
      }

      val ftr = fileDownloaderGroup.withSource(file.toURI.toString, { is => new GZIPInputStream(is) }) { source =>
        source.getLines().mkString("\n")
      }

      ftr.map(body => new String(body) must_=== body).await
    }
  }
}
