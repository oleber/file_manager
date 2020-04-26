# File Manager

Work with files where ever they are

# Why did I do this work?

I have 2 reasons:
 * It was fun
 * After many years using Scala to process files. Normal issues:
   * Files are available locally or remotely via http, s3, ...
   * The files need to be closed
   * The files may to be process asynchronously
   * Unit test is also a pain
   * ... 

# Code Examples

## Reading the number of lines in a files
```scala
import scala.concurrent.Future
import com.oleber.filemanager.FileDownloader.fileDownloaderGroup

def load(path: String): Future[Int] = {
    val result = fileDownloaderGroup.withSource(path) {source =>
        source.getLines().size()
    }
    
    result match {
      case Some(ftr) =>
        // process future
      case None => throw new Exception("Can't open file")
    }
}

// local file
load("local/path")

// local file, with URI
load("file:/local/path")

// For testing, you may read from a string
load("string://line1\nline2")

// http file
load("http://example.com")
```

## Reading a GZip file
```scala
import java.util.zip.GZIPInputStream
import scala.concurrent.Future
import com.oleber.filemanager.FileDownloader.fileDownloaderGroup

def load(path: String): Future[Int] = {
    val result = fileDownloaderGroup.withSource(path, {is => new GZIPInputStream()}) {source =>
        source.getLines().size()
    }
    
    result match {
      case Some(ftr) =>
        // process future
      case None => throw new Exception("Can't open file")
    }
}

// local file
load("local/path")

// local file, with URI
load("file:/local/path")

// For testing, you may read from a string
load("string://line1\nline2")

// http file
load("http://example.com")
```

