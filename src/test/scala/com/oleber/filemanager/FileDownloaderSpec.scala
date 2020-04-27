package com.oleber.filemanager

import java.io.{File, PrintStream}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

import FileDownloader.{URLFileDownloader, allFileDownloaderGroup}
import com.oleber.filemanager.FileCloserEnvironment.closeOnExit
import com.oleber.filemanager.FileUploader.fileUploaderGroup
import com.oleber.filemanager.FileUtils.withTempDirectory
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import org.specs2.specification.core.Fragments

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

    "ResourceFileWorker" in {
      val ftr = fileDownloaderGroup.doWith("classpath:ResourceFileWorker.txt") { is =>
        Source.fromInputStream(is).getLines().mkString
      }

      ftr.map(_ must be_===("some text")).await
    }

    "URLFileDownloader" in {
      val file = File.createTempFile("temp", null)
      file.deleteOnExit()

      fileUploaderGroup.doWithSync(file.toString, { os => new PrintStream(os) }) {
        _.print("some text: João")
      }

      val ftr = fileDownloaderGroup.slurp(file.toURI.toString)

      ftr.map(body => new String(body) must_=== "some text: João").await
    }

    "ResourceFileWorker" in {
      val ftr = fileDownloaderGroup.doWith("classpath:ResourceFileWorker.txt") { is =>
        Source.fromInputStream(is).getLines().mkString
      }

      ftr.map(_ must be_===("some text")).await
    }

    "BashFileDownloader" in {
      val ftr = allFileDownloaderGroup.slurp("bash: echo 'some text: João'")

      ftr.map(array => new String(array).trim must be_===("some text: João")).await
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


    val URLFileDownloaderRegexp = List(
      "http://www.goofle.com" -> true,
      "https://www.goofle.com" -> true,
      "file:/home/foo/xxx.txt" -> true,
      "/home/foo/xxx.txt" -> false,
      "jar:/foo/bar" -> true
    )

    Fragments.foreach(URLFileDownloaderRegexp) { case (str, expected) =>
      s"URLFileDownloader regexp: '$str' must_=== $expected" in {
        URLFileDownloader.regex.pattern.matcher(str).matches() must be_===(expected)
      }
    }

    "FileFileDownloader" in withTempDirectory { path =>
      val file = path.resolve("foo.txt")

      closeOnExit(new PrintStream(file.toFile)) {
        _.print("some text: João")
      }

      val ftr = fileDownloaderGroup.slurp(file.toString)

      ftr.map(body => new String(body) must_=== "some text: João").await
    }
  }
}
