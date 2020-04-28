package com.oleber.filemanager.fileCommons

import scala.util.matching.Regex

trait FileFileCommons {
  def validPath(path: String, excludeRegexp: Option[Regex], includeRegexp: Option[Regex]): Boolean = {
    val includeOK = includeRegexp.forall(_.findFirstIn(path).isDefined)

    val excludeOK = excludeRegexp.forall(_.findFirstIn(path).isEmpty)

    includeOK && excludeOK
  }
}
