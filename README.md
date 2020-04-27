# File Manager

Work with files where ever they are. The code shall avoid use of useless external 
libraries. For use cases like S3, ftp, ... , the most popular library shall be used
and provided (this code shall avoid version locking).

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
  fileDownloaderGroup.doWith(path) {inputStream =>
    Source.fromInputStream(inputStream).getLines().size
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

When the method doWith ends the InputStream is closed.
 
#### Process the file

```scala
  fileDownloaderGroup.doWith(path) {inputStream =>
    Source.fromInputStream(inputStream).getLines().size
  }
```

#### Multiple file formats are readable (more to come)

```scala
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

In Scala, reading from a Source is more common case. We have a function that wraps the 
InputStream on a Source object.

```scala  
import scala.concurrent.Future
import com.oleber.filemanager.FileDownloader.fileDownloaderGroup

val result: Future[Int] = fileDownloaderGroup.withSource(path) {_.getLines().size}
```

### Reading a GZip file

The some kind of code may be used to read other formats. The InputStream transformation
is available all over  

```scala
import java.util.zip.GZIPInputStream
import scala.concurrent.Future
import com.oleber.filemanager.FileDownloader.fileDownloaderGroup

val result: Future[Int] = fileDownloaderGroup
  .withSource("path/to/file", {is => new GZIPInputStream(is)}) {
    _.getLines().size
  }
```

### Slurp all file in a go

```scala
import scala.concurrent.Future
import com.oleber.filemanager.FileDownloader.fileDownloaderGroup

val result: Future[Array[Byte]] = fileDownloaderGroup.slurp("path/to/file")
```

### Use bash as InputStream
```scala
import scala.concurrent.Future
import com.oleber.filemanager.FileDownloader.allFileDownloaderGroup

val result: Future[Array[Byte]] = allFileDownloaderGroup.slurp("bash: echo 'Hello world!!!'")
```

With big power comes big responsibility. Many tasks may be done on the bash without use of 
other Java libraries. 

**Note:** Running commands of Bash is far from safe, so one new variable was create.
Bash actions are relegated to `allFileDownloaderGroup`. `allFileDownloaderGroup` shall have
all the `FileDownloader`'s on `fileDownloaderGroup` and a few that look more unsafe ones.

### Adding more FileDownloader is easy

Following is the code for reading a file on the resources, you may always build your own.

```scala
  import com.oleber.filemanager.{FileDownloader, BlockingFuture}

  object ResourceFileDownloader extends FileDownloader {
    val regex = "classpath:(.*)".r

    override def open(path: String)(implicit ec: ExecutionContext): Option[Future[InputStream]] = {
      path match {
        case regex(matchPath) =>
          Some(BlockingFuture {
            getClass.getClassLoader.getResource(matchPath).openStream()
          })
        case _ => None
      }
    }
  }
```

You will need to build your FileDownloaderGroup. The order of FileDownloader's is the 
one that is going to be followed to find a FileDownloader suitable. For many use cases
you can use the one available on FileDownloader.fileDownloaderGroup

```scala
  val fileDownloaderGroup = new FileDownloaderGroup(
    StringFileDownloader,
    ResourceFileDownloader,
    URLFileDownloader,
    FileFileDownloader
  )
```
 
You also need to be able to close the connection. For this you may need to provide a 
FileCloser. The code already provides the support for AutoCloseable with a code as:

```scala
  implicit object AutoCloseableFileCloser extends FileCloser[AutoCloseable] {
    override def close(autoCloseable: AutoCloseable): Unit = autoCloseable.close()
  }
``` 

TODO: All methods