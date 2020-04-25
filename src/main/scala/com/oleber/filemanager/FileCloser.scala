package com.oleber.filemanager

trait FileCloser[-T] {
  def close(t: T): Unit
}

object FileCloser {
  implicit object AutoCloseableFileCloser extends FileCloser[AutoCloseable] {
    override def close(t: AutoCloseable): Unit = t.close()
  }
}
