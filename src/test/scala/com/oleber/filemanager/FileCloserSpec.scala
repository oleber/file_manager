package com.oleber.filemanager

import org.specs2.mutable.Specification

class FileCloserSpec extends Specification {
  "FileCloser" should {
    "AutoCloseableFileCloser" in {
      var closed = false
      implicitly[FileCloser[AutoCloseable]].close(new AutoCloseable() {
        override def close(): Unit = closed = true
      })

      closed must beTrue
    }
  }

}
