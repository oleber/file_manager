package com.oleber.filemanager.fileDownloader

import com.oleber.filemanager.FileDownloader.{allFileDownloaderGroup}
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

class ResourceFileDownloaderSpec(implicit ee: ExecutionEnv) extends Specification {
  "ResourceFileDownloader" should {
    "ResourceFileDownloader" in {
      val buffer = allFileDownloaderGroup.slurpSync("classpath:ResourceFileWorker.txt")

      new String(buffer).trim must_=== "some text"
    }
  }
}
