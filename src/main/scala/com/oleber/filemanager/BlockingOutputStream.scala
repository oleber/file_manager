package com.oleber.filemanager

import java.io.OutputStream

import scala.concurrent.blocking

class BlockingOutputStream(os: OutputStream) extends OutputStream {
  override def write(i: Int): Unit = blocking {
    os
  }

  override def write(b: Array[Byte]): Unit = blocking {
    os.write(b)
  }

  override def write(b: Array[Byte], off: Int, len: Int): Unit = blocking {
    os.write(b, off, len)
  }

  override def flush(): Unit = blocking {
    os.flush()
  }

  override def close(): Unit = blocking {
    os.close()
  }
}
