package com.oleber.filemanager

import java.io.PrintStream
import java.nio.file.Path

import com.oleber.filemanager.FileCloserEnvironment.closeOnExit
import com.oleber.filemanager.FileUtils.{walk, withTempDirectory}
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

import scala.concurrent.Future

class FileUtilsSpec(implicit ee: ExecutionEnv) extends Specification {
  def printFile(path: Path, body: String) = {
    closeOnExit(new PrintStream(path.toFile)) { printStream =>
      printStream.print(body)
    }
  }

  "FileUtils" should {
    "withTempDirectory" in {
      var collectPath = Option.empty[Path]
      val result = withTempDirectory{ path =>
        collectPath = Some(path)

        val topFile = path.resolve("text.txt")
        val subDirectory = path.resolve("subPath")
        val subFile = subDirectory.resolve("text.txt")

        subDirectory.toFile.mkdir()
        printFile(topFile, "some text")
        printFile(subFile, "some text")

        path.toFile.isDirectory must beTrue
        path.resolve("text.txt").toFile.isFile must beTrue
        path.resolve("foo.txt").toFile.isFile must beFalse
        subDirectory.resolve("text.txt").toFile.isFile must beTrue

        10
      }

      result must_=== 10
      collectPath.isDefined must beTrue
      collectPath.get.toFile.exists() must beFalse
      collectPath.get.toFile.isDirectory must beFalse
    }

    "walk" in withTempDirectory{ path =>
      val topFile = path.resolve("text.txt")
      val subDirectory = path.resolve("subPath")
      val subDirectoryFile1 = subDirectory.resolve("text1.txt")
      val subDirectoryFile2 = subDirectory.resolve("text2.txt")

      subDirectory.toFile.mkdir()

      printFile(topFile, "text")
      printFile(subDirectoryFile1, "text")
      printFile(subDirectoryFile2, "text")

      walk(path, true).toList must_=== List(path, subDirectory, subDirectoryFile1, subDirectoryFile2, topFile)
      walk(path, false).toList must_=== List(subDirectoryFile1, subDirectoryFile2, subDirectory, topFile, path)
      walk(path).toList must_=== List(subDirectoryFile1, subDirectoryFile2, subDirectory, topFile, path)
    }

    "closeOnExit sync" in {
      // given
      var executed = false
      case class Foo()
      implicit object FooFileCloser extends FileCloser[Foo] {
        override def close(t: Foo): Unit = {
          executed = true
        }
      }

      // when
      val result = closeOnExit(Foo()) { foo =>
        foo must beAnInstanceOf[Foo]
        executed must beFalse
        1
      }

      executed must beTrue
      result must_=== 1
    }

    "closeOnExit assync" in {
      // given
      var executed = false
      case class Foo()
      implicit object FooFileCloser extends FileCloser[Foo] {
        override def close(t: Foo): Unit = {
          executed = true
        }
      }

      // when
      val resultFtr = closeOnExit(Foo()) { foo =>
        Future {
          Thread.sleep(100)
          foo must beAnInstanceOf[Foo]
          executed must beFalse
          1
        }
      }

      // then
      executed must beFalse

      for {
        result <- resultFtr
      } yield {
        executed must beTrue
        result must_=== 1
      }
    }
  }
}
