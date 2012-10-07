package jp.sf.amateras.play2.httpsession

import com.codahale.jerkson.Json
import play.api.mvc._

/**
 * Provides an interface to access javax.servlet.http.HttpSession.
 */
case class HttpSession(private[httpsession] val operations: List[Any] = Nil) {

  /**
   * Retrieves the object from this session.
   */
  def apply[T](key: String)(implicit h: RequestHeader, m: scala.reflect.Manifest[T]): Option[T] = {
    if(HttpSessionHelpers.isLocalSession){
      HttpSessionHelpers.session.getAttribute(key) match {
        case null => None
        // deserialize the session object from JSON in the development mode
        case json: String => Some(new Json{}.parse[T](json))
      }
    } else {
      HttpSessionHelpers.session.getAttribute(key) match {
        case null => None
        case value => Some(value.asInstanceOf[T])
      }
    }
  }
  
  /**
   * Returns the session id.
   */
  def id(implicit h: RequestHeader): String = HttpSessionHelpers.session.getId

  /**
   * Adds the object to this session.
   */
  def +[T <: AnyRef](kv: (String, T))(implicit m: scala.reflect.Manifest[T]): HttpSession = {
    HttpSession(operations :+ (kv._1, kv._2))
  }

  /**
   * Removes the object from this session.
   */
  def -(key: String): HttpSession = {
    new HttpSession(operations :+ key)
  }

}

