package jp.sf.amateras.play2.httpsession

import scala.collection.JavaConverters._
import com.codahale.jerkson.Json
import play.api.mvc._

/**
 * Provides APIs for Play2 applications.
 * 
 * You can use javax.servlet.http.HttpSession in Play2 applications to import members of this object as following:
 * {{{
 * import jp.sf.amateras.play2.httpsession.HttpSessionSupport._
 * 
 * def index = Action { implicit request =>
 *   val count = (HttpSession[String]("counter") match {
 *     case None    => 0
 *     case Some(i) => i.toInt
 *   }) + 1
 *
 *   Ok("count=%d".format(count)).withHttpSession {
 *     "counter" -> count.toString
 *   }
 * }
 * }}}
 */
object HttpSessionSupport {

  lazy val HttpSession = new HttpSessionImpl()

  implicit def withHttpSession[T <: Result](result: T) = new ResultWrapper(result)

  class ResultWrapper(val result: Result) {
    def withHttpSession(session: (String, AnyRef)*)(implicit requestHeader: RequestHeader): Result = {
      val httpSession = HttpSessionHelpers.session
      httpSession.getAttributeNames.asScala.foreach { name =>
        httpSession.removeAttribute(name)
      }
      session.foreach { case (key, value) =>
        if(HttpSessionHelpers.isLocalSession){
          // serializes the session object as JSON in the development mode
          httpSession.setAttribute(key, new Json{}.generate(value))
        } else {
          httpSession.setAttribute(key, value)
        }
      }
      result
    }

    def withHttpSession(session: HttpSessionImpl)(implicit requestHeader: RequestHeader): Result = {
      session.operations.foreach {
        case (key: String) => HttpSessionHelpers.session.removeAttribute(key)
        case (key: String, value: AnyRef) => {
          if(HttpSessionHelpers.isLocalSession){
            // serializes the session object as JSON
            HttpSessionHelpers.session.setAttribute(key, new Json{}.generate(value))
            // puts value into cache if session cache is available
            cacheMap.foreach(_.put(key, value))
          } else {
            HttpSessionHelpers.session.setAttribute(key, value)
          }
        }
      }
      result
    }
  }
  
  /**
   * Mutable Map for the session cache in the development mode.
   */
  private var cacheMap: Option[scala.collection.mutable.Map[String, Object]] = None
  
  /**
   * Starts the session cache in the development mode.
   * Do nothing in the production mode.
   */
  def startSessionCache(implicit requestHeader: RequestHeader): Unit = {
    if(HttpSessionHelpers.isLocalSession){
      cacheMap = Some(scala.collection.mutable.Map())
    }
  }
  
  /**
   * Ends the session cache in the development mode.
   * Do nothing in the production mode.
   */
  def endSessionCache(implicit requestHeader: RequestHeader): Unit = {
    if(HttpSessionHelpers.isLocalSession){
      cacheMap = None
    }
  } 
  
  def withSessionCache[A](requestHeader: RequestHeader)(f: => A): A = {
    startSessionCache(requestHeader)
    try {
      f
    } finally {
      endSessionCache(requestHeader)
    }
  }
  
  /**
   * Provides an interface to access javax.servlet.http.HttpSession.
   */
  case class HttpSessionImpl(private[httpsession] val operations: List[Any] = Nil) {

    /**
     * Retrieves the object from this session.
     */
    def apply[T](key: String)(implicit h: RequestHeader, m: scala.reflect.Manifest[T]): Option[T] = {
      if(HttpSessionHelpers.isLocalSession){
        HttpSessionHelpers.session.getAttribute(key) match {
          case null => None
          case json: String => cacheMap.map(_.get(key)).getOrElse(None) match {
            // returns from cache
            case Some(value) => Some(value.asInstanceOf[T])
            // deserialize the session object from JSON
            case None => {
              val value = Some(new Json{}.parse[T](json))
              // puts value into cache if session cache is available
              cacheMap.foreach(_.put(key, value.get.asInstanceOf[Object]))
              value
            }
          }
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
    def +[T <: AnyRef](kv: (String, T))(implicit m: scala.reflect.Manifest[T]): HttpSessionImpl = {
      HttpSessionImpl(operations :+ (kv._1, kv._2))
    }

    /**
     * Removes the object from this session.
     */
    def -(key: String): HttpSessionImpl = {
      new HttpSessionImpl(operations :+ key)
    }

  }
}
