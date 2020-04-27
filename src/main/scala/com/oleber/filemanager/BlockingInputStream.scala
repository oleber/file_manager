package com.oleber.filemanager

import java.io.{InputStream, OutputStream}

import scala.concurrent.{ExecutionContext, blocking}

class BlockingInputStream(is: InputStream)(implicit ec: ExecutionContext) extends InputStream {
  override def read(): Int = blocking {
    is.read()
  }

  override def read(b: Array[Byte]): Int = blocking {
    is.read(b)
  }

  override def read(b: Array[Byte], off: Int, len: Int): Int = blocking {
    is.read(b, off, len)
  }

  override def readAllBytes(): Array[Byte] = blocking {
    is.readAllBytes()
  }

  override def readNBytes(len: Int): Array[Byte] = blocking {
    is.readNBytes(len)
  }

  override def readNBytes(b: Array[Byte], off: Int, len: Int): Int = blocking {
    is.readNBytes(b, off, len)
  }

  override def skip(n: Long): Long = blocking {
    is.skip(n)
  }

  override def available(): Int = blocking {
    is.available()
  }

  override def close(): Unit = blocking {
    is.close()
  }

  override def mark(readlimit: Int): Unit = blocking {
    is.mark(readlimit)
  }

  override def reset(): Unit = blocking {
    is.reset()
  }

  override def markSupported(): Boolean = blocking {
    is.markSupported()
  }

  override def transferTo(out: OutputStream): Long = blocking {
    is.transferTo(out)
  }
}
