package com.oleber.filemanager

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

import scala.concurrent.{Future, Promise}

class FileCloserEnvironmentSpec(implicit ee: ExecutionEnv) extends Specification {

  class MyClass {
    var closed = 0

    def close(): Unit = {
      closed += 1
    }
  }

  implicit val fileCloser: FileCloser[MyClass] = (t: MyClass) => t.close()

  "FileCloserEnvironment" should {
    "work sync" in {
      val myClass = new MyClass

      val result = FileCloserEnvironment.closeOnExit(myClass) { _ => 10 }

      result must_=== 10

      myClass.closed must_=== 1
    }

    "work async" in {
      val myClass = new MyClass

      val result = FileCloserEnvironment.closeOnExit(myClass) { _ =>
        Future {
          Thread.sleep(100)
          10
        }
      }

      val testFtr = result.map(_ must_=== 10)
      myClass.closed must_=== 0
      testFtr.await

      myClass.closed must_=== 1
    }
  }
}
