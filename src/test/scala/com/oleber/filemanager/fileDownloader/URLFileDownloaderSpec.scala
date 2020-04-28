package com.oleber.filemanager.fileDownloader

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import org.specs2.specification.core.Fragments

class URLFileDownloaderSpec(implicit ee: ExecutionEnv) extends Specification {
  "URLFileDownloader" should {
    val URLFileDownloaderRegexp = List(
      "http://www.goofle.com" -> true,
      "https://www.goofle.com" -> true,
      "file:/home/foo/xxx.txt" -> false,
      "/home/foo/xxx.txt" -> false,
      "jar:/foo/bar" -> true
    )

    Fragments.foreach(URLFileDownloaderRegexp) { case (str, expected) =>
      s"URLFileDownloader regexp: '$str' must_=== $expected" in {
        URLFileDownloader.regex.pattern.matcher(str).matches() must be_===(expected)
      }
    }
  }
}
