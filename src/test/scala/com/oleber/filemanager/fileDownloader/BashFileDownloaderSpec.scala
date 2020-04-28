package com.oleber.filemanager.fileDownloader

import com.oleber.filemanager.FileDownloader.allFileDownloaderGroup
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

class BashFileDownloaderSpec(implicit ee: ExecutionEnv) extends Specification {
  "BashFileDownloader" should {
    "BashFileDownloader" in {
      val buffer = allFileDownloaderGroup.slurpSync("bash: echo 'some text: João'")

      new String(buffer).trim must_=== "some text: João"
    }
  }
}
