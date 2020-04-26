# File Manager

Work with files where ever they are

# Why did I do this work?

Working with Files was always a pain, here are some reasons for building this code:
 * It was fun
 * After many years using Scala to process files. Normal issues:
   * Files are available locally or remotely via http, s3, ...
   * The files need to be closed
   * The files may to be process asynchronously
   * Unit test is also a pain
   * ... 

# Code Examples

## Reading Files

The examples will read the number of lines on a file.

### Reading from an InputStream

```scala
import scala.concurrent.Future
import scala.io.Source
import com.oleber.filemanager.FileDownloader.fileDownloaderGroup

def load(path: String): Future[Int] = {
  val result = fileDownloaderGroup.doWith(path) {inputStream =>
    Source.fromInputStream(inputStream).getLines().size
  }
    
  result.getOrElse(throw new Exception("Can't open file"))
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

### Reading from a Source

In Scala, reading from a Source is more common.

```scala
import scala.concurrent.Future
import com.oleber.filemanager.FileDownloader.fileDownloaderGroup

def load(path: String): Future[Int] = {
    val result = fileDownloaderGroup.withSource(path) {source =>
        source.getLines().size
    }
    
    result.getOrElse(throw new Exception("Can't open file"))
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

### Reading a GZip file

The some kind of code may be used to read other formats.

```scala
import java.util.zip.GZIPInputStream
import scala.concurrent.Future
import com.oleber.filemanager.FileDownloader.fileDownloaderGroup

def load(path: String): Future[Int] = {
    val result = fileDownloaderGroup.withSource(path, {is => new GZIPInputStream(is)}) {source =>
        source.getLines().size
    }
    
    result.getOrElse(throw new Exception("Can't open file"))
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

### Slurp all file in a go

```scala
import scala.concurrent.Future
import com.oleber.filemanager.FileDownloader.fileDownloaderGroup

def load(path: String): Future[Array[Byte]] = {
    val result = fileDownloaderGroup.slurp(path)
    
    result.getOrElse(throw new Exception("Can't open file"))
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
