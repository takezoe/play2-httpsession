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

/**
 * Provides APIs for Play2 applications.
 * 
 * You can use javax.servlet.http.HttpSession in Play2 applications to import members of this object as following:
 * {{{
 * import jp.sf.amateras.play2.httpsession.HttpSessionSupport._
 * 
 * def index = Action { implicit request =>
 *   val count = (HttpSession[Int]("counter") match {
 *     case None    => 0
 *     case Some(i) => i
 *   }) + 1
 *
 *   Ok("count=%d".format(count)).withHttpSession {
 *     "counter" -> count
 *   }
 * }
 * }}}
 */
object HttpSessionSupport {

  lazy val HttpSession = new HttpSession()

  implicit def withHttpSession[T <: Result](result: T) = new ResultWrapper(result)

  class ResultWrapper(val result: Result) {
    def withHttpSession(session: (String, AnyRef)*)(implicit requestHeader: RequestHeader): Result = {
      HttpSessionHelpers.session.invalidate()
      session.foreach { case (key, value) =>
        if(HttpSessionHelpers.isLocalSession){
          HttpSessionHelpers.session.setAttribute(key, new Json{}.generate(value))
        } else {
          HttpSessionHelpers.session.setAttribute(key, value)
        }
      }
      result
    }

    def withHttpSession(session: HttpSession)(implicit requestHeader: RequestHeader): Result = {
      session.operations.foreach {
        case (key: String) => HttpSessionHelpers.session.removeAttribute(key)
        case (key: String, value: AnyRef) => {
          if(HttpSessionHelpers.isLocalSession){
            HttpSessionHelpers.session.setAttribute(key, new Json{}.generate(value))
          } else {
            HttpSessionHelpers.session.setAttribute(key, value)
          }
        }
      }
      result
    }
  }
}
