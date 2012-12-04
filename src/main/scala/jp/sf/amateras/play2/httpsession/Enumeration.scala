package jp.sf.amateras.play2.httpsession

/**
 * Provides java.util.Enumeration interface for scala.collection.Seq.
 */
class Enumeration(values: Seq[String]) extends java.util.Enumeration[String] {
  var index = -1
  
  def hasMoreElements(): Boolean = index < values.length - 1
              
  def nextElement(): String = {
    index = index + 1
    values(index)
  }
}