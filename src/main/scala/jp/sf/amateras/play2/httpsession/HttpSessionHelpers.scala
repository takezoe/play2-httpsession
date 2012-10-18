package jp.sf.amateras.play2.httpsession

import play.api.mvc._
import scala.collection.mutable._

import javax.servlet._
import javax.servlet.http._
import javax.servlet.http.{HttpSession => ServletSession}

/**
 * Provides helpers used in HttpSession support internally.
 */
object HttpSessionHelpers {
  
  private[httpsession] val sessionMap = new HashMap[String, ServletSession] with SynchronizedMap[String, ServletSession]

  private[httpsession] def setHttpSession(session: ServletSession) = {
    sessionMap.put(session.getId(), session)
  }
  
  private[httpsession] def removeHttpSession(session: ServletSession) = {
    sessionMap.remove(session.getId())
  }
  
  /**
   * Returns the session id.
   */
  private[httpsession] def sessionId(implicit requestHeader: RequestHeader): Option[String] = 
    requestHeader.queryString.get("HTTP_SESSION") match {
      case None|Some(Nil) => None
      case Some(Seq(sessionId)) => Some(sessionId)
    }
  
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