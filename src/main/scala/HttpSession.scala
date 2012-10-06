package jp.sf.amateras.play2.httpsession

import scala.collection.JavaConverters._
import scala.collection.mutable._
import javax.servlet.http._
import javax.servlet.http.{HttpSession => ServletSession}
import javax.servlet._
import javax.servlet.annotation._
import com.codahale.jerkson.Json
import play.api.mvc._
import org.codehaus.jackson.map.ObjectMapper

/**
 * Removes HttpSession from SessionHolder#sessionMap when a session is destroyed.
 */
@WebListener
class HttpSessionListener extends javax.servlet.http.HttpSessionListener {

  def sessionCreated(event: HttpSessionEvent): Unit = HttpSessionHelpers.setHttpSession(event.getSession())

  def sessionDestroyed(event: HttpSessionEvent): Unit = HttpSessionHelpers.removeHttpSession(event.getSession())

}

/**
 * Puts HttpSession into SessionHolder#sessionMap.
 */
@WebFilter(urlPatterns = Array("/*"))
class HttpSessionFilter extends Filter {

  def init(config: FilterConfig): Unit = {}
  def destroy(): Unit = {}

  def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {
    request match {
      case httpRequest: HttpServletRequest => {
        val session = httpRequest.getSession()
        try {
          HttpSessionHelpers.setHttpSession(session)
          
          val wrappedRequest = new HttpServletRequestWrapper(httpRequest){
            override def getParameterMap(): java.util.Map[String, Array[String]] =
              (super.getParameterMap().asScala ++ Map("HTTP_SESSION" -> Array("TRUE"))).asJava
          }
          chain.doFilter(wrappedRequest, response)
        } finally {
          HttpSessionHelpers.removeHttpSession(session)
        }
      }
    }
  }

}

/**
 * Provides helpers used in HttpSession support internally.
 */
object HttpSessionHelpers {
  
  private[httpsession] val sessionIds = new ThreadLocal[String]
  
  private[httpsession] val sessionMap = new HashMap[String, ServletSession] with SynchronizedMap[String, ServletSession]

  private[httpsession] def setHttpSession(session: ServletSession) = {
    sessionIds.set(session.getId())
    sessionMap.put(session.getId(), session)
  }
  
  private[httpsession] def removeHttpSession(session: ServletSession) = {
    sessionIds.remove
    sessionMap.remove(session.getId())
  }
  
  /**
   * Returns the session id.
   */
  private[httpsession] def sessionId: Option[String] = Option(sessionIds.get)
  
  /**
   * Tests whether the current session is a local session.
   */
  private[httpsession] def isLocalSession(implicit requestHeader: RequestHeader): Boolean =
	!requestHeader.queryString.contains("HTTP_SESSION")

  /**
   * Returns the javax.servlet.http.HttpSession.
   */
  private[httpsession] def session(implicit requestHeader: RequestHeader): ServletSession = {
    sessionId match {
      case Some(x) => sessionMap(x)
      case None    => sessionMap.getOrElseUpdate("LOCAL_SESSION", // Should it identify for each client...?
        new ServletSession(){
          val attributes = new HashMap[String, Object]

          def getCreationTime(): Long = throw new UnsupportedOperationException
          def getId(): String = "LOCAL_SESSION"
          def getLastAccessedTime(): Long = throw new UnsupportedOperationException
          def getServletContext(): ServletContext = throw new UnsupportedOperationException
          def setMaxInactiveInterval(i: Int): Unit = throw new UnsupportedOperationException
          def getMaxInactiveInterval(): Int = throw new UnsupportedOperationException
          def getSessionContext(): HttpSessionContext = throw new UnsupportedOperationException
          def getValue(s: String): Object = throw new UnsupportedOperationException
          def getAttributeNames(): java.util.Enumeration[String] = throw new UnsupportedOperationException
          def getValueNames(): Array[String] = throw new UnsupportedOperationException
          def putValue(s: String, obj: Object): Unit = throw new UnsupportedOperationException
          def removeValue(s: String): Unit = throw new UnsupportedOperationException
          def isNew(): Boolean = throw new UnsupportedOperationException

          def getAttribute(s: String): Object = attributes.get(s).orNull
          def setAttribute(s: String, obj: Object): Unit = attributes.put(s, obj)
          def removeAttribute(s: String): Unit = attributes.remove(s)
          def invalidate(): Unit = sessionMap.remove("LOCAL_SESSION")
        }
      )
    }
  }

}

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
